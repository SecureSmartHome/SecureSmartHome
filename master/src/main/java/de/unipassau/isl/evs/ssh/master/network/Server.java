package de.unipassau.isl.evs.ssh.master.network;


import android.content.Context;
import android.content.SharedPreferences;

import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.StartupException;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.network.ClientIncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.network.handler.TimeoutHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultExecutorServiceFactory;
import io.netty.util.concurrent.GlobalEventExecutor;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.CLIENT_ALL_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.CLIENT_READER_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.CLIENT_WRITER_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.DEFAULT_PORT;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.FILE_SHARED_PREFS;
import static de.unipassau.isl.evs.ssh.master.MasterConstants.PREF_SERVER_PORT;

/**
 * The heart of the master server: a netty stack accepting connections from devices and handling communication with them using a netty pipeline.
 * Additionally, it keeps track of timeouts and holds the global connection registry.
 * For details about the pipeline, see {@link #startServer()} and {@link #initChannel(SocketChannel)}.
 */
public class Server extends AbstractComponent {
    public static final Key<Server> KEY = new Key<>(Server.class);
    private static final String TAG = Server.class.getSimpleName();
    /**
     * The EventLoopGroup used for accepting connections
     */
    private EventLoopGroup serverExecutor;
    /**
     * The channel listening for incoming connections on the port of the server.
     * Use {@link ChannelFuture#sync()} to wait for server startup.
     */
    private ChannelFuture serverChannel;

    /**
     * A ChannelGroup containing really <i>all</i> incoming connections.
     * If it isn't contained here, it is not connected to the Server.
     */
    private ChannelGroup connections;

    /**
     * The ObjectEncoder shared by all pipelines used for serializing all sent {@link de.unipassau.isl.evs.ssh.core.messaging.Message}s
     */
    private ObjectEncoder sharedObjectEncoder = new ObjectEncoder();
    /**
     * The ObjectDecoder shared by all pipelines used for deserializing all sent {@link de.unipassau.isl.evs.ssh.core.messaging.Message}s
     */
    private ObjectDecoder sharedObjectDecoder = new ObjectDecoder(
            ClassResolvers.weakCachingConcurrentResolver(ClassLoader.getSystemClassLoader()));
    /**
     * Distributes incoming messages to the responsible handlers.
     */
    private ServerIncomingDispatcher incomingDispatcher = new ServerIncomingDispatcher(this);

    /**
     * Init timeouts and the connection registry and start the netty IO server synchronously
     */
    @Override
    public void init(Container container) {
        super.init(container);
        try {
            //TODO require device registry
            startServer();
            container.register(IncomingDispatcher.KEY, incomingDispatcher);
        } catch (InterruptedException e) {
            throw new StartupException("Could not start netty server", e);
        }
    }

    /**
     * Scotty, start me up!
     * Initializes the netty data pipeline and starts the IO server
     *
     * @throws InterruptedException     if interrupted while waiting for the startup
     * @throws IllegalArgumentException is the Server is already running
     */
    private void startServer() throws InterruptedException {
        if (isChannelOpen() && isExecutorAlive()) {
            throw new IllegalStateException("Server already running");
        }

        ResourceLeakDetector.setLevel(getResourceLeakDetection());

        //Setup the Executor and Connection Pool
        serverExecutor = new NioEventLoopGroup(0, new DefaultExecutorServiceFactory("server"));
        connections = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

        ServerBootstrap b = new ServerBootstrap()
                .group(serverExecutor)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(final SocketChannel ch) throws Exception {
                        Server.this.initChannel(ch);
                    }
                })
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        //Wait for the start of the server
        serverChannel = b.bind(getPort()).sync();
        if (serverChannel == null) {
            throw new StartupException("Could not open server channel");
        }
    }

    /**
     * Configures the per-connection pipeline that is responsible for handling incoming and outgoing data.
     * After an incoming packet is decrypted, decoded and verified,
     * it will be sent to its target {@link de.unipassau.isl.evs.ssh.core.handler.Handler}
     * by the {@link ClientIncomingDispatcher}.
     */
    protected void initChannel(SocketChannel ch) throws GeneralSecurityException {
        Container container = getContainer();
        if (!isActive() || container == null) {
            //Do not accept new connections after the Server has been shut down
            ch.close();
            return;
        }

        //TODO setup pipeline
        //Handler (de-)serialization
        ch.pipeline().addLast(sharedObjectEncoder.getClass().getSimpleName(), sharedObjectEncoder);
        ch.pipeline().addLast(sharedObjectDecoder.getClass().getSimpleName(), sharedObjectDecoder);
        ch.pipeline().addLast(LoggingHandler.class.getSimpleName(), new LoggingHandler(LogLevel.TRACE));
        //Timeout Handler
        ch.pipeline().addLast(IdleStateHandler.class.getSimpleName(),
                new IdleStateHandler(CLIENT_READER_IDLE_TIME, CLIENT_WRITER_IDLE_TIME, CLIENT_ALL_IDLE_TIME));
        ch.pipeline().addLast(TimeoutHandler.class.getSimpleName(), new TimeoutHandler());
        //Dispatcher
        ch.pipeline().addLast(ClientIncomingDispatcher.class.getSimpleName(), incomingDispatcher);

        //Register connection
        connections.add(ch);
    }

    /**
     * Stop listening, close all connections and shut down the executors.
     */
    public void destroy() {
        if (serverChannel != null && serverChannel.channel() != null) {
            serverChannel.channel().close();
        }
        if (serverExecutor != null) {
            serverExecutor.shutdownGracefully(1, 5, TimeUnit.SECONDS); //TODO config grace duration
        }
        getContainer().unregister(incomingDispatcher);
        super.destroy();
    }


    /**
     * Finds the Channel that is contained in a pipeline of a netty IO channel matching the given ID.
     *
     * @return the found Channel, or {@code null} if no Channel matches the given ID
     */
    public Channel findChannel(DeviceID id) {
        for (Channel channel : connections) {
            if (channel.isActive()
                //&& channel.id().asShortText().startsWith(id)
                //&& channel.attr(AttributeKey.newInstance("device-uid")).matches(id) //TODO also match device UID
                    ) {
                return channel;
            }
        }
        return null;
    }

    private int getPort() {
        SharedPreferences sharedPref = getComponent(ContainerService.KEY_CONTEXT)
                .getSharedPreferences(FILE_SHARED_PREFS, Context.MODE_PRIVATE);
        return sharedPref.getInt(PREF_SERVER_PORT, DEFAULT_PORT);
    }

    private ResourceLeakDetector.Level getResourceLeakDetection() {
        return ResourceLeakDetector.Level.PARANOID;
    }

    /**
     * EventLoopGroup for the ClientIncomingDispatcher and the ClientOutgoingRouter
     */
    EventLoopGroup getExecutor() {
        return serverExecutor;
    }

    /**
     * Channel for the ClientIncomingDispatcher and the ClientOutgoingRouter
     */
    Channel getChannel() {
        return serverChannel != null ? serverChannel.channel() : null;
    }

    /**
     * @return the local Address this server is listening on
     */
    public SocketAddress getAddress() {
        return serverChannel.channel().localAddress();
    }

    /**
     * @return the ChannelGroup containing <b>all</b> currently open connections
     */
    public ChannelGroup getActiveChannels() {
        return connections;
    }

    /**
     * @return {@code true}, if the Server TCP channel is currently open
     */
    public boolean isChannelOpen() {
        return serverChannel != null && serverChannel.channel() != null && serverChannel.channel().isOpen();
    }

    /**
     * @return {@code true}, if the Executor that is used for accepting incoming connections and processing data
     * has been shut down
     */
    public boolean isExecutorAlive() {
        return serverExecutor != null && !serverExecutor.isTerminated() && !serverExecutor.isShutdown();
    }

    /**
     * Blocks until the Server channel has been closed.
     *
     * @throws InterruptedException
     * @see #isChannelOpen()
     * @see Channel#closeFuture()
     */
    public void awaitShutdown() throws InterruptedException {
        serverChannel.channel().closeFuture().await();
        serverExecutor.terminationFuture().await();
    }
}