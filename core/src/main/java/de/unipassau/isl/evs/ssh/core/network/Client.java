package de.unipassau.isl.evs.ssh.core.network;


import android.content.SharedPreferences;
import android.util.Log;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.network.handler.TimeoutHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import io.netty.util.concurrent.DefaultExecutorServiceFactory;

import static android.content.Context.MODE_PRIVATE;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.CLIENT_ALL_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.CLIENT_MAX_DISCONNECTS;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.CLIENT_MILLIS_BETWEEN_DISCONNECTS;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.CLIENT_READER_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.CLIENT_WRITER_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.DEFAULT_PORT;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.FILE_SHARED_PREFS;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.PREF_HOST;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.PREF_PORT;

/**
 * A netty stack accepting connections to and from the master and handling communication with them using a netty pipeline.
 * For details about the pipeline, see {@link #startClient()} and {@link #initChannel(SocketChannel)}.
 */
public class Client extends AbstractComponent {
    public static final Key<Client> KEY = new Key<>(Client.class);
    private static final String TAG = Client.class.getSimpleName();
    private static final Level RESOURCE_LEAK_DETECTION = Level.PARANOID;
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
    private EventLoopGroup clientExecutor;
    /**
     * The channel listening for incoming TCP connections on the port of the client.
     * Use {@link ChannelFuture#sync()} to wait for client startup.
     */
    private ChannelFuture channel;
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
     * Init timeouts and the connection registry.
     * Calls {@link #startClient()} method and to start the netty IO client synchronously.
     */
    @Override
    public void init(Container container) {
        super.init(container);
        container.register(IncomingDispatcher.KEY, incomingDispatcher);
        container.register(OutgoingRouter.KEY, outgoingRouter);
        container.register(UDPDiscoveryClient.KEY, udpDiscovery);
        startClient();
        isActive = true;
    }

    /**
     * Initializes the netty data pipeline and starts the client.
     *
     * @throws IllegalArgumentException is the Client is already running
     */
    protected void startClient() {
        Log.d(TAG, "startClient");
        if (!isActive) {
            Log.w(TAG, "Not starting Client that has been shut-down");
            return;
        }
        if (isChannelOpen() && isExecutorAlive()) {
            Log.w(TAG, "Not starting Client that is already connected");
            return;
        }
        if (!isExecutorAlive()) {
            // Setup the Executor and Connection Pool
            clientExecutor = new NioEventLoopGroup(0, new DefaultExecutorServiceFactory("client"));
        }
        ResourceLeakDetector.setLevel(RESOURCE_LEAK_DETECTION);
        final String host = getSharedPrefs().getString(PREF_HOST, null);
        final int port = getSharedPrefs().getInt(PREF_PORT, DEFAULT_PORT);
        if (disconnectsInARow < CLIENT_MAX_DISCONNECTS && host != null) {
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

    protected void connectClient(String host, int port) {
        Log.i(TAG, "Client connecting to " + host + ":" + port);
        // TCP Connection
        Bootstrap b = new Bootstrap()
                .group(clientExecutor)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        Client.this.initChannel(ch);
                    }
                })
                .option(ChannelOption.SO_KEEPALIVE, true);

        // Wait for the start of the client
        channel = b.connect(host, port);
        channel.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                Log.v(TAG, "Channel open");
                // TODO when implemented, start handshake
            }
        });
        channel.channel().closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (isActive && isExecutorAlive() && !clientExecutor.isShuttingDown()) {
                    long time = System.currentTimeMillis();
                    if (lastDisconnect <= 0 || time - lastDisconnect <= CLIENT_MILLIS_BETWEEN_DISCONNECTS) {
                        lastDisconnect = time;
                        disconnectsInARow++;
                        Log.w(TAG, disconnectsInARow + ". disconnect within the last " + lastDisconnect + "ms, retrying");
                    } else {
                        Log.i(TAG, "Regular disconnect, retrying");
                        disconnectsInARow = 0;
                    }
                    startClient();
                }
            }
        });
    }

    /**
     * Configures the per-connection pipeline that is responsible for handling incoming and outgoing data.
     * After an incoming packet is decrypted, decoded and verified,
     * it will be sent to its target {@link de.unipassau.isl.evs.ssh.core.handler.MessageHandler}
     * by the {@link ClientIncomingDispatcher}.
     */
    protected void initChannel(SocketChannel ch) throws GeneralSecurityException {
        Log.v(TAG, "initChannel"); //TODO add remaining necessary handlers, when they are implemented

        //Handler (de-)serialization
        ch.pipeline().addLast(ObjectEncoder.class.getSimpleName(), new ObjectEncoder());
        ch.pipeline().addLast(ObjectDecoder.class.getSimpleName(), new ObjectDecoder(
                ClassResolvers.weakCachingConcurrentResolver(getClass().getClassLoader())));
        ch.pipeline().addLast(LoggingHandler.class.getSimpleName(), new LoggingHandler(LogLevel.TRACE));

        //Timeout Handler
        ch.pipeline().addLast(IdleStateHandler.class.getSimpleName(),
                new IdleStateHandler(CLIENT_READER_IDLE_TIME, CLIENT_WRITER_IDLE_TIME, CLIENT_ALL_IDLE_TIME));
        ch.pipeline().addLast(TimeoutHandler.class.getSimpleName(), new TimeoutHandler());

        //Dispatcher
        ch.pipeline().addLast(ClientIncomingDispatcher.class.getSimpleName(), incomingDispatcher);
    }

    void onDiscoverySuccessful(InetAddress address, int port) {
        Log.i(TAG, "UDP discovery successful, found " + address + ":" + port);
        getSharedPrefs().edit()
                .putString(PREF_HOST, address.getHostName())
                .putInt(PREF_PORT, port)
                .commit();
        startClient();
    }

    /**
     * Stop listening, close all connections and shut down the executors.
     */
    public void destroy() {
        Log.d(TAG, "stopClient");
        isActive = false;
        if (channel != null && channel.channel() != null) {
            channel.channel().close();
        }
        if (clientExecutor != null) {
            clientExecutor.shutdownGracefully(1, 5, TimeUnit.SECONDS);
        }
        getContainer().unregister(udpDiscovery);
        getContainer().unregister(outgoingRouter);
        getContainer().unregister(incomingDispatcher);
        super.destroy();
    }

    //GETTERS///////////////////////////////////////////////////////////////////////////////////////////////////////////

    private SharedPreferences getSharedPrefs() {
        return getComponent(ContainerService.KEY_CONTEXT).getSharedPreferences(FILE_SHARED_PREFS, MODE_PRIVATE);
    }

    /**
     * EventLoopGroup for the ClientIncomingDispatcher and the ClientOutgoingRouter
     */
    EventLoopGroup getExecutor() {
        return clientExecutor;
    }

    /**
     * Channel for the ClientIncomingDispatcher and the ClientOutgoingRouter
     */
    Channel getChannel() {
        return channel != null ? channel.channel() : null;
    }

    /**
     * @return the local Address this client is listening on
     */
    public SocketAddress getAddress() {
        return channel.channel().localAddress();
    }

    /**
     * @return {@code true}, if the Client TCP channel is currently open
     */
    public boolean isChannelOpen() {
        return channel != null && channel.channel() != null && channel.channel().isOpen();
    }

    /**
     * @return {@code true}, if the Executor that is used for processing data has been shut down
     */
    public boolean isExecutorAlive() {
        return clientExecutor != null && !clientExecutor.isTerminated() && !clientExecutor.isShutdown();
    }

    /**
     * Blocks until the Client channel has been closed.
     *
     * @throws InterruptedException
     */
    public void awaitShutdown() throws InterruptedException {
        channel.channel().closeFuture().await();
        clientExecutor.terminationFuture().await();
    }
}
