package de.unipassau.isl.evs.ssh.core.network;


import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.StartupException;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.network.handler.ClientBroadcastHandler;

import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.network.handler.TimeoutHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultExecutorServiceFactory;

import static android.content.Context.MODE_PRIVATE;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.CLIENT_ALL_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.CLIENT_READER_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.CLIENT_WRITER_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.DEFAULT_PORT;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.DEFAULT_TIMEOUTS;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.FILE_SHARED_PREFS;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.MAX_NUMBER_OF_TIMEOUTS;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.MAX_SECONDS_BETWEEN_BROADCAST;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.MIN_SECONDS_BETWEEN_TIMEOUTS;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.PREF_HOST;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.PREF_PORT;


/**
 * A netty stack accepting connections to and from the master and handling communication with them using a netty pipeline.
 * For details about the pipeline, see {@link #startClient()} and {@link #initChannel(SocketChannel)}.
 */
public class Client extends AbstractComponent {
    public static final Key<Client> KEY = new Key<>(Client.class);
    private static final String TAG = Client.class.getSimpleName();
    /**
     * Receives messages from system components and decides how to route them to the targets.
     */
    private final ClientOutgoingRouter outgoingRouter = new ClientOutgoingRouter();
    /**
     * The EventLoopGroup used for accepting connections
     */
    private EventLoopGroup clientExecutor;
    /**
     * The channel listening for incoming TCP connections on the port of the client.
     * Use {@link ChannelFuture#sync()} to wait for client startup.
     */
    private ChannelFuture tcpChannel;
    /**
     * The channel listening for incoming UDP connections on the port of the client.
     * Use {@link ChannelFuture#sync()} to wait for client startup.
     */
    private ChannelFuture udpChannel;
    /**
     * Distributes incoming messages to the responsible handlers.
     */
    private ClientIncomingDispatcher incomingDispatcher = new ClientIncomingDispatcher(this);
    /**
     * SharedPreferences to load, save and edit key-value sets.
     */
    private SharedPreferences prefs;
    /**
     * SharedPreferences editor to edit the key-value sets.
     */
    private SharedPreferences.Editor editor;
    /**
     * TODO javadoc
     */
    private WifiManager wifi;
    /**
     * TODO javadoc
     */
    private WifiManager.MulticastLock multicastLock;
    /**
     * Boolean if the client connection is active.
     */
    private boolean isActive;
    /**
     * Int used to calculate the time between the last and the current timeout.
     */
    private long lastTimeout = 0;
    /**
     * Int that saves how many timeouts happened in a row.
     */
    private int timeoutsInARow = DEFAULT_TIMEOUTS;

    private Context context;

    public Client(Context context) {
        this.context = context;
    }

    /**
     * Init timeouts and the connection registry.
     * Calls {@link #startClient()} method and to start the netty IO client synchronously.
     */
    @Override
    public void init(Container container) {
        super.init(container);
        try {
            startClient();
            container.register(IncomingDispatcher.KEY, incomingDispatcher);
            container.register(OutgoingRouter.KEY, outgoingRouter);
            isActive = true;
        } catch (InterruptedException e) {
            throw new StartupException("Could not start netty client", e);
        }
    }

    /**
     * Initializes the netty data pipeline and starts the client.
     *
     * @throws InterruptedException     if interrupted while waiting for the startup
     * @throws IllegalArgumentException is the Client is already running
     */
    private void startClient() throws InterruptedException {
        if (isTCPChannelOpen() && isExecutorAlive()) {
            throw new IllegalStateException("client already running");
        }

        ResourceLeakDetector.setLevel(getResourceLeakDetection());

        if (!isExecutorAlive()) {
            // Setup the Executor and Connection Pool
            clientExecutor = new NioEventLoopGroup(0, new DefaultExecutorServiceFactory("client"));
        }
        if (timeoutsInARow < MAX_NUMBER_OF_TIMEOUTS) {
            buildTCP();
            // TODO when implemented, start handshake
        } else {
            broadcastUDP();
        }
    }

    private void buildTCP() throws InterruptedException {
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
        tcpChannel = b.connect(getHost(), getPort());
        if (tcpChannel == null) {
            throw new StartupException("Could not open client channel");
        }
        tcpChannel.channel().closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (isActive && isExecutorAlive() && !clientExecutor.isShuttingDown()) {
                    long thisTimeout = System.currentTimeMillis();
                    if (lastTimeout == 0 || ((thisTimeout - lastTimeout) >= TimeUnit.SECONDS.toMillis(MIN_SECONDS_BETWEEN_TIMEOUTS))) {
                        lastTimeout = thisTimeout;
                        timeoutsInARow++;
                    } else {
                        timeoutsInARow = 0;
                    }
                    startClient();
                }
            }
        });
    }

    private void broadcastUDP() throws InterruptedException {
        wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            multicastLock = wifi.createMulticastLock("");
            multicastLock.acquire();
        }
        Bootstrap b = new Bootstrap()
                .group(clientExecutor)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        initBroadcastChannel(ch);
                    }
                })
                .option(ChannelOption.SO_BROADCAST, true);
        // Send broadcast
        udpChannel = b.bind(getPort());
        getChannel().writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer("REQUEST", CharsetUtil.UTF_8),
                new InetSocketAddress(CoreConstants.BROADCAST_ADDRESS, DEFAULT_PORT)));
        // Check for timeout
        clientExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                if (!isUDPClientConnected()) {
                    try {
                        broadcastUDP();// restart broadcast
                    } catch (InterruptedException e) {
                        e.printStackTrace();//TODO
                    }
                }
            }
        }, MAX_SECONDS_BETWEEN_BROADCAST, TimeUnit.SECONDS);
    }

    public void receivedUDPResponse(String address, int port) {
        if (multicastLock != null) {
            multicastLock.release();
            multicastLock = null;
        }
        if (prefs == null) {
            PreferenceManager.getDefaultSharedPreferences(getContainer().get(ContainerService.KEY_CONTEXT));
        }
        if (editor == null) {
            editor = prefs.edit();
        }
        editor.putString(PREF_HOST, address);
        editor.putInt(PREF_PORT, port);
        editor.apply();
        timeoutsInARow = 0;

    }

    /**
     * Configures the per-connection pipeline that is responsible for handling incoming and outgoing data.
     * After an incoming packet is decrypted, decoded and verified,
     * it will be sent to its target {@link de.unipassau.isl.evs.ssh.core.handler.Handler}
     * by the {@link ClientIncomingDispatcher}.
     */
    protected void initChannel(SocketChannel ch) throws GeneralSecurityException {
        //TODO add remaining necessary handlers, when they are implemented
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

    /**
     * Configures the broadcast sent when 3 timeouts occur by sending the packet to {@link ClientBroadcastHandler}
     * by using the {@link ClientIncomingDispatcher}
     */
    private void initBroadcastChannel(SocketChannel ch) {
        ch.pipeline().addLast(LoggingHandler.class.getSimpleName(), new LoggingHandler(LogLevel.TRACE));
        //ClientBroadcastHandler
        ch.pipeline().addLast(ClientBroadcastHandler.class.getSimpleName(), new ClientBroadcastHandler(this));
    }

    /**
     * Stop listening, close all connections and shut down the executors.
     */
    public void destroy() {
        isActive = false;
        if (tcpChannel != null && tcpChannel.channel() != null) {
            tcpChannel.channel().close();
        }
        if (clientExecutor != null) {
            clientExecutor.shutdownGracefully(1, 5, TimeUnit.SECONDS);
        }
        getContainer().unregister(incomingDispatcher);
        getContainer().unregister(outgoingRouter);
        super.destroy();
    }

    /**
     * @return if available host name, otherwise {@code null}.
     */
    private String getHost() {
        SharedPreferences sharedPref = getComponent(ContainerService.KEY_CONTEXT)
                .getSharedPreferences(FILE_SHARED_PREFS, MODE_PRIVATE);
        return sharedPref.getString(PREF_HOST, null);
    }

    /**
     * @return if available port, otherwise default port
     */
    private int getPort() {
        SharedPreferences sharedPref = getComponent(ContainerService.KEY_CONTEXT)
                .getSharedPreferences(FILE_SHARED_PREFS, MODE_PRIVATE);
        return sharedPref.getInt(PREF_PORT, DEFAULT_PORT);
    }

    private ResourceLeakDetector.Level getResourceLeakDetection() {
        return ResourceLeakDetector.Level.PARANOID;
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
        return tcpChannel != null ? tcpChannel.channel() : null;
    }

    /**
     * @return the local Address this client is listening on
     */
    public SocketAddress getAddress() {
        return tcpChannel.channel().localAddress();
    }

    /**
     * @return {@code true}, if the Client TCP channel is currently open
     */
    public boolean isTCPChannelOpen() {
        return tcpChannel != null && tcpChannel.channel() != null && tcpChannel.channel().isOpen();
    }

    private boolean isUDPClientConnected() {
        return false;//TODO
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
        tcpChannel.channel().closeFuture().await();
        clientExecutor.terminationFuture().await();
    }
}
