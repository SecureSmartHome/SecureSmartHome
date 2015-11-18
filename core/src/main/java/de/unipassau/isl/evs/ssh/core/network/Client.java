package de.unipassau.isl.evs.ssh.core.network;


import android.content.SharedPreferences;

import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.StartupException;
import io.netty.bootstrap.Bootstrap;
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
import io.netty.util.concurrent.DefaultExecutorServiceFactory;

import static android.content.Context.MODE_PRIVATE;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.CLIENT_ALL_IDLE_TIME;
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
    /**
     * The EventLoopGroup used for accepting connections
     */
    private EventLoopGroup clientExecutor;
    /**
     * The channel listening for incoming connections on the port of the client.
     * Use {@link ChannelFuture#sync()} to wait for client startup.
     */
    private ChannelFuture clientChannel;
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
     * Boolean if the client connection is active.
     */
    private boolean isActive;

    /**
     * Init timeouts and the connection registry.
     * Calls {@link #startClient()} method and to start the netty IO client synchronously.
     */
    @Override
    public void init(Container container) {
        super.init(container);
        try {
            startClient();
            isActive = true;
        } catch (InterruptedException e) {
            throw new StartupException("Could not start netty client", e);
        }
    }

    /**
     * Initializes the netty data pipeline and starts the client
     *
     * @throws InterruptedException     if interrupted while waiting for the startup
     * @throws IllegalArgumentException is the Client is already running
     */
    private void startClient() throws InterruptedException {
        // TODO clientChannel not initialized yet -> NullPointerException without (clientChannel != null)
        if (clientChannel != null && isChannelOpen() && isExecutorAlive()) {
            throw new IllegalStateException("client already running");
        }

        ResourceLeakDetector.setLevel(getResourceLeakDetection());

        if (!isExecutorAlive()) {
            //Setup the Executor and Connection Pool
            clientExecutor = new NioEventLoopGroup(0, new DefaultExecutorServiceFactory("client"));
        }

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

        //Wait for the start of the client
        clientChannel = b.connect(getHost(), getPort()).sync();
        clientChannel.channel().closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (isActive && isExecutorAlive() && !clientExecutor.isShuttingDown()) {
                    startClient();
                    //FIXME this may result in an endless loop if the hostname is wrong or not reachable (e.g. due to connection loss)
                }
            }
        });
        if (clientChannel == null) {
            throw new StartupException("Could not open client channel");
        }
    }

    /**
     * Configures the per-connection pipeline that is responsible for handling incoming and outgoing data.
     * After an incoming packet is decrypted, decoded and verified,
     * it will be sent to its target {@link de.unipassau.isl.evs.ssh.core.handler.Handler}
     * by the {@link de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher}.
     */
    protected void initChannel(SocketChannel ch) throws GeneralSecurityException {
        //TODO add remaining necessary handlers

        //Handler (de-)serialization
        ch.pipeline().addLast(sharedObjectEncoder.getClass().getSimpleName(), sharedObjectEncoder);
        ch.pipeline().addLast(sharedObjectDecoder.getClass().getSimpleName(), sharedObjectDecoder);
        ch.pipeline().addLast(LoggingHandler.class.getSimpleName(), new LoggingHandler(LogLevel.TRACE));
        //Timeout Handler
        ch.pipeline().addLast(IdleStateHandler.class.getSimpleName(),
                new IdleStateHandler(CLIENT_READER_IDLE_TIME, CLIENT_WRITER_IDLE_TIME, CLIENT_ALL_IDLE_TIME));
        ch.pipeline().addLast(TimeoutHandler.class.getSimpleName(), new TimeoutHandler());
    }

    /**
     * Stop listening, close all connections and shut down the executors.
     */
    public void destroy() {
        isActive = false;
        if (clientChannel != null && clientChannel.channel() != null) {
            clientChannel.channel().close();
        }
        if (clientExecutor != null) {
            clientExecutor.shutdownGracefully(1, 5, TimeUnit.SECONDS);
        }
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
     * @return the local Address this client is listening on
     */
    public SocketAddress getAddress() {
        return clientChannel.channel().localAddress();
    }

    /**
     * @return {@code true}, if the Client TCP channel is currently open
     */
    public boolean isChannelOpen() {
        return clientChannel.channel() != null && clientChannel.channel().isOpen();
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
        clientChannel.channel().closeFuture().await();
        clientExecutor.terminationFuture().await();
    }
}
