package de.unipassau.isl.evs.ssh.core.mock.network;


import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.network.ClientIncomingDispatcher;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.net.SocketAddress;
import java.security.GeneralSecurityException;

/**
 * Mock Class for the Client Class to be able to use it for tests
 *
 * @author Chris
 */
public class ClientMock extends Client {

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
     * Distributes incoming messages to the responsible handlers.
     */
    private ClientIncomingDispatcher incomingDispatcher = new ClientIncomingDispatcher();

    /**
     * Boolean if the client connection is active.
     */
    private boolean isActive;

    /**
     * Init timeouts and the connection registry.
     */
    @Override
    public void init(Container container) {
        super.init(container);
        container.register(IncomingDispatcher.KEY, incomingDispatcher);
        isActive = true;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws GeneralSecurityException {
    }

    /**
     * Stop listening, close all connections and shut down the executors.
     */
    @Override
    public void destroy() {
        isActive = false;
        getContainer().unregister(incomingDispatcher);
        super.destroy();
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
        return clientChannel != null ? clientChannel.channel() : null;
    }

    /**
     * @return the local Address this client is listening on
     */
    @Override
    public SocketAddress getAddress() {
        return clientChannel.channel().localAddress();
    }

    /**
     * @return {@code true}, if the Client TCP channel is currently open
     */
    @Override
    public boolean isChannelOpen() {
        return clientChannel != null && clientChannel.channel() != null && clientChannel.channel().isOpen();
    }

    /**
     * @return {@code true}, if the Executor that is used for processing data has been shut down
     */
    @Override
    public boolean isExecutorAlive() {
        return clientExecutor != null && !clientExecutor.isTerminated() && !clientExecutor.isShutdown();
    }

    /**
     * Blocks until the Client channel has been closed.
     *
     * @throws InterruptedException
     */
    @Override
    public void awaitShutdown() throws InterruptedException {
    }

}