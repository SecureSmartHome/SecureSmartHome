package de.unipassau.isl.evs.ssh.core.network;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import java.net.InetAddress;
import java.net.SocketAddress;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultExecutorServiceFactory;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;

import static android.content.Context.MODE_PRIVATE;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.CLIENT_MAX_DISCONNECTS;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.CLIENT_MILLIS_BETWEEN_DISCONNECTS;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.DEFAULT_PORT;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.FILE_SHARED_PREFS;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.PREF_HOST;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.PREF_PORT;

/**
 * A netty stack accepting connections to and from the master and handling communication with them using a netty pipeline.
 * For details about the pipeline, see {@link #initChannel(SocketChannel)}.
 * For details about switching to UDP discovery, see {@link #initClient()} and {@link #shouldReconnectTCP()}.
 * This component is used by the Slave and the end-user android App.
 *
 * @author Phil
 */
public class Client extends AbstractComponent {
    public static final Key<Client> KEY = new Key<>(Client.class);

    private static final String TAG = Client.class.getSimpleName();

    /**
     * Receives messages from system components and decides how to route them to the targets.
     */
    private final ClientOutgoingRouter outgoingRouter = new ClientOutgoingRouter();
    /**
     * Boolean if the client connection is active.
     */
    private boolean isActive;
    /**
     * The EventLoopGroup used for accepting connections
     */
    private EventLoopGroup executor;
    /**
     * The channel listening for incoming TCP connections on the port of the client.
     * Use {@link ChannelFuture#sync()} to wait for client startup.
     */
    private ChannelFuture channelFuture;
    /**
     * Distributes incoming messages to the responsible handlers.
     */
    private ClientIncomingDispatcher incomingDispatcher = new ClientIncomingDispatcher();
    /**
     * Send UDP Broadcasts when the Server can't be reached
     */
    private UDPDiscoveryClient udpDiscovery = new UDPDiscoveryClient();
    /**
     * Int used to calculate the time between the last and the current timeout.
     */
    private long lastDisconnect = 0;
    /**
     * Int that saves how many timeouts happened in a row.
     */
    private int disconnectsInARow = 0;

    /**
     * Configure netty and add related Components to the Container.
     * Afterwards call {@link #initClient()} method to start the netty IO client asynchronously.
     */
    @Override
    public void init(Container container) {
        super.init(container);
        // Configure netty
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory() {
            @Override
            public InternalLogger newInstance(String name) {
                return new NettyInternalLogger(name);
            }
        });
        ResourceLeakDetector.setLevel(CoreConstants.RESOURCE_LEAK_DETECTION);
        // Add related components
        container.register(IncomingDispatcher.KEY, incomingDispatcher);
        container.register(OutgoingRouter.KEY, outgoingRouter);
        container.register(UDPDiscoveryClient.KEY, udpDiscovery);
        // And try to connect
        isActive = true;
        initClient();
    }

    /**
     * Initializes the netty client and tries to connect to the server.
     * If the connect ist successful, {@link #initChannel(SocketChannel)} is called in order to add the
     * required Handlers to the pipeline.
     * If the connection fails, {@link #channelClosed(Channel)} is called until to many retries are made and the Client
     * switches to searching the master via UDP discovery using the {@link UDPDiscoveryClient}.
     */
    protected synchronized void initClient() {
        Log.d(TAG, "initClient");
        if (!isActive) {
            Log.w(TAG, "Not starting Client that has been explicitly shut-down");
            return;
        }
        if (isChannelOpen() && isExecutorAlive()) {
            Log.w(TAG, "Not starting Client that is already connected");
            return;
        } else {
            // Close channels open from previous connections
            if (channelFuture != null && channelFuture.channel() != null) {
                Log.v(TAG, "Cleaning up " + (channelFuture.channel().isOpen() ? "open" : "closed")
                        + " Channel from previous connection attempt");
                channelFuture.channel().close();
            }
            // Clean-up the closed channel as it is no longer needed
            channelFuture = null;
        }
        if (!isExecutorAlive()) {
            // Setup the Executor and Connection Pool
            executor = new NioEventLoopGroup(0, new DefaultExecutorServiceFactory("client"));
        }
        // Read the previous host and port from the shared preferences
        final String host = getSharedPrefs().getString(PREF_HOST, null);
        final int port = getSharedPrefs().getInt(PREF_PORT, DEFAULT_PORT);

        // And queue the (re-)connect
        executor.submit(new Runnable() {
            @Override
            public void run() {
                // Connect to TCP if the address of the Server/Master is known and not too many connection attempts have failed
                if (host != null && shouldReconnectTCP()) {
                    connectClient(host, port);
                } else {
                    if (host == null) {
                        Log.w(TAG, "No master known, starting UDP discovery");
                    } else {
                        Log.w(TAG, "Too many disconnects from " + host + ":" + port + ", trying UDP discovery");
                    }
                    requireComponent(UDPDiscoveryClient.KEY).startDiscovery();
                }
            }
        });
    }

    /**
     * A connection attempt should be made if no more than CLIENT_MAX_DISCONNECTS attempts have been made
     * with the last attempt having been made no more than CLIENT_MILLIS_BETWEEN_DISCONNECTS ago.
     */
    private boolean shouldReconnectTCP() {
        return disconnectsInARow < CLIENT_MAX_DISCONNECTS
                || System.currentTimeMillis() - lastDisconnect > CLIENT_MILLIS_BETWEEN_DISCONNECTS;
    }

    /**
     * Tries to establish a TCP connection to the Server with the given host and port.
     */
    protected void connectClient(String host, int port) {
        Log.i(TAG, "Client connecting to " + host + ":" + port);

        // TCP Connection
        Bootstrap b = new Bootstrap()
                .group(executor)
                .channel(NioSocketChannel.class)
                .handler(getHandshakeHandler())
                .option(ChannelOption.SO_KEEPALIVE, true);

        // Wait for the start of the client
        channelFuture = b.connect(host, port);
        channelFuture.addListener(new ChannelFutureListener() {
            /**
             * Called once the operation completes, either because the connect was successful or because of an error.
             */
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    Log.v(TAG, "Channel open");
                    channelOpen(future.channel());
                } else {
                    Log.v(TAG, "Channel open failed");
                    channelClosed(future.channel());
                }
            }
        });
        channelFuture.channel().closeFuture().addListener(new ChannelFutureListener() {
            /**
             * Called once the connection is closed.
             */
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                Log.v(TAG, "Channel closed");
                channelClosed(future.channel());
            }
        });
    }

    /**
     * HandshakeHandle can be changed or mocked for testing
     *
     * @return the ClientHandshakeHandler to use
     */
    @NonNull
    protected ClientHandshakeHandler getHandshakeHandler() {
        return new ClientHandshakeHandler(this, getContainer());
    }

    /**
     * Called once the TCP connection is established.
     */
    protected void channelOpen(Channel channel) {
        requireComponent(UDPDiscoveryClient.KEY).stopDiscovery();
    }

    /**
     * Called once the TCP connection is closed or if it couldn't be established at all.
     * Increments the disconnect counter and tries to re-establish the connection.
     */
    protected synchronized void channelClosed(Channel channel) {
        if (channel != this.channelFuture.channel()) {
            return; //channel has already been exchanged by new one, don't start another client
        }
        if (isActive && isExecutorAlive() && !executor.isShuttingDown()) {
            long time = System.currentTimeMillis();
            final long diff = time - lastDisconnect;
            if (lastDisconnect <= 0 || diff <= CLIENT_MILLIS_BETWEEN_DISCONNECTS) {
                // if the last disconnect was in the near past, increment the counter of disconnects in a row
                lastDisconnect = time;
                disconnectsInARow++;
                Log.w(TAG, disconnectsInARow + ". disconnect within the last " + diff + "ms, retrying");
            } else {
                // otherwise just retry
                Log.i(TAG, "Regular disconnect, retrying");
            }
            initClient();
        } else {
            Log.i(TAG, "Client disconnected, but not restarting because destroy() has been called");
        }
    }

    /**
     * Called by {@link UDPDiscoveryClient} once it found a possible address of the master.
     * Saves the new address
     */
    void onDiscoverySuccessful(InetAddress address, int port) {
        Log.i(TAG, "UDP discovery successful, found " + address + ":" + port);
        getSharedPrefs().edit()
                .putString(PREF_HOST, address.getHostName())
                .putInt(PREF_PORT, port)
                .commit();
        lastDisconnect = 0;
        disconnectsInARow = 0;
        initClient();
    }

    /**
     * Stop listening, close all connections and shut down the executors.
     */
    public void destroy() {
        Log.d(TAG, "stopClient");
        isActive = false;
        if (channelFuture != null && channelFuture.channel() != null) {
            channelFuture.channel().close();
        }
        if (executor != null) {
            executor.shutdownGracefully();
        }
        getContainer().unregister(udpDiscovery);
        getContainer().unregister(outgoingRouter);
        getContainer().unregister(incomingDispatcher);
        super.destroy();
    }

    /**
     * EventLoopGroup for the {@link ClientIncomingDispatcher} and the {@link ClientOutgoingRouter}
     */
    EventLoopGroup getExecutor() {
        return executor;
    }

    /**
     * Channel for the {@link ClientIncomingDispatcher} and the {@link ClientOutgoingRouter}
     */
    Channel getChannel() {
        return channelFuture != null ? channelFuture.channel() : null;
    }

    /**
     * {@link IncomingDispatcher} that will be registered to the Pipeline by the {@link ClientHandshakeHandler}
     */
    ClientIncomingDispatcher getIncomingDispatcher() {
        return incomingDispatcher;
    }

    private SharedPreferences getSharedPrefs() {
        return getComponent(ContainerService.KEY_CONTEXT).getSharedPreferences(FILE_SHARED_PREFS, MODE_PRIVATE);
    }

    /**
     * @return the local Address this client is listening on
     */
    public SocketAddress getAddress() {
        if (channelFuture == null || channelFuture.channel() == null) {
            return null;
        } else {
            return channelFuture.channel().localAddress();
        }
    }

    /**
     * @return {@code true}, if the Client TCP channel is currently open
     */
    public boolean isChannelOpen() {
        return channelFuture != null && channelFuture.channel() != null && channelFuture.channel().isOpen();
    }

    /**
     * @return {@code true}, if the Executor that is used for processing data has not been shut down
     */
    public boolean isExecutorAlive() {
        return executor != null && !executor.isTerminated() && !executor.isShutdown();
    }

    /**
     * Blocks until the Client has been completely shut down.
     *
     * @throws InterruptedException
     */
    public void awaitShutdown() throws InterruptedException {
        channelFuture.channel().closeFuture().await();
        executor.terminationFuture().await();
    }
}
