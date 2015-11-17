package de.unipassau.isl.evs.ssh.core.network;


import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.StartupException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
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
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultExecutorServiceFactory;

/**
 * FIXME javadoc
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
     * Init timeouts and the connection registry and start the netty IO server synchronously
     * FIXME javadoc
     */
    @Override
    public void init(Container container) {
        super.init(container);
        try {
            //TODO read timeouts from config
            //TODO require device registry
            startClient();
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
        if ((clientExecutor != null && !clientExecutor.isTerminated())) {
            throw new IllegalStateException("client already running");
        }

        setResourceLeakDetection();

        //Setup the Executor and Connection Pool
        clientExecutor = new NioEventLoopGroup(0, new DefaultExecutorServiceFactory("client"));

        //TODO Configure the Client via the Bootstrap
        Bootstrap b = new Bootstrap()
                .group(clientExecutor)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true);

        //Wait for the start of the client
        clientChannel = b.connect(getHost(), getPort()).sync();
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


        //TODO setup pipeline
        //Handler (de-)serialization
        ch.pipeline().addLast(sharedObjectEncoder);
        ch.pipeline().addLast(sharedObjectDecoder);
        ch.pipeline().addLast(new LoggingHandler(LogLevel.TRACE));
    }

    /**
     * Stop listening, close all connections and shut down the executors.
     */
    public void destroy() {
        if (clientChannel != null && clientChannel.channel() != null) {
            clientChannel.channel().close();
        }
        if (clientExecutor != null) {
            clientExecutor.shutdownGracefully(1, 5, TimeUnit.SECONDS); //TODO config grace duration
        }
        super.destroy();
    }

    private String getHost() {
        return "hello"; // TODO get host data
    }

    private int getPort() {
        return 12345; // TODO get data from database;
    }

    private void setResourceLeakDetection() {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
    }

    /**
     * @return the local Address this client is listening on
     */
    public SocketAddress getAddress() {
        return clientChannel.channel().localAddress();
    }

    /**
     * @return {@code true}, if the Client TCP channel isn't open currently
     */
    public boolean isChannelInactive() {
        return clientChannel.channel() == null || !clientChannel.channel().isActive();
    }

    /**
     * @return {@code true}, if the Executor that is used for accepting incoming connections and processing data
     * has been shut down
     */
    public boolean isExecutorTerminated() {
        return clientExecutor.isTerminated();
    }

    /**
     * Blocks until the Client channel has been closed.
     *
     * @throws InterruptedException
     * @see #isChannelInactive()
     * @see Channel#closeFuture()
     */
    public void awaitShutdown() throws InterruptedException {
        clientChannel.channel().closeFuture().await();
    }
}
