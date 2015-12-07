package de.unipassau.isl.evs.ssh.master.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.test.InstrumentationTestCase;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.network.ClientHandshakeHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;

public class ServerTest extends InstrumentationTestCase {
    public void testStartup() throws IOException, InterruptedException {
        SimpleContainer container = new SimpleContainer();
        addContext(container);
        addServer(container);
        shutdownServer(container);
    }

    public void testConnect() throws IOException, InterruptedException {
        SimpleContainer container = new SimpleContainer();
        addContext(container);
        Server server = addServer(container);
        try {
            Socket client1 = new Socket("localhost", ((InetSocketAddress) server.getAddress()).getPort());
            Thread.sleep(1000); //wait for the connection to be established

            assertTrue(client1.isConnected());
            assertEquals(1, server.getActiveChannels().size());

            client1.close();
            Thread.sleep(1000); //wait for the connection to be closed

            assertEquals(0, server.getActiveChannels().size());
        } finally {
            shutdownServer(container);
        }
    }

    public void testConnectClient() throws IOException, InterruptedException {
        SimpleContainer serverContainer = new SimpleContainer();
        addContext(serverContainer);
        Server server = addServer(serverContainer);
        try {
            SimpleContainer clientContainer = new SimpleContainer();
            addContext(clientContainer);
            clientContainer.register(Client.KEY, new Client());
            Thread.sleep(1000); //wait for the connection to be established

            Client client = clientContainer.get(Client.KEY);
            assertTrue(client.isChannelOpen());
            assertTrue(client.isExecutorAlive());
            assertEquals(1, server.getActiveChannels().size());

            clientContainer.unregister(Client.KEY);
            client.awaitShutdown();

            assertFalse(client.isChannelOpen());
            assertNotNull(client.getAddress());
            assertFalse(client.isExecutorAlive());
            assertEquals(0, server.getActiveChannels().size());
        } finally {
            shutdownServer(serverContainer);
        }
    }

    public void testSerialRoundTrip() throws InterruptedException, ExecutionException {
        final BlockingQueue<Object> serverQueue = new LinkedBlockingQueue<>();
        final BlockingQueue<Object> clientQueue = new LinkedBlockingQueue<>();
        final Promise<SocketChannel> serverChannel = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
        final Promise<SocketChannel> clientChannel = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

        SimpleContainer serverContainer = new SimpleContainer();
        addContext(serverContainer);
        Server server = new TestServer(serverQueue, serverChannel);
        serverContainer.register(Server.KEY, server);
        try {
            SimpleContainer clientContainer = new SimpleContainer();
            addContext(clientContainer);
            Client client = new TestClient(clientQueue, clientChannel);
            clientContainer.register(Client.KEY, client);

            try {
                serverChannel.await(1000);
                clientChannel.await(1000);

                runRoundTripTests(serverQueue, clientQueue, serverChannel, clientChannel);
            } finally {
                clientContainer.unregister(Client.KEY);
                client.awaitShutdown();
            }
        } finally {
            shutdownServer(serverContainer);
        }
        assertTrue(serverQueue.isEmpty());
        assertTrue(clientQueue.isEmpty());
    }

    private void runRoundTripTests(BlockingQueue<Object> serverQueue, BlockingQueue<Object> clientQueue,
                                   Promise<SocketChannel> serverChannel, Promise<SocketChannel> clientChannel)
            throws InterruptedException, ExecutionException {
        String obj1 = "test123ÄÖÜ∑";
        clientChannel.get().writeAndFlush(obj1).await(1000);
        assertEquals(obj1, serverQueue.poll(1000, TimeUnit.MILLISECONDS));

        Map<String, Object> obj2 = new HashMap<>();
        obj2.put("test", "abc");
        obj2.put("test2", 123);
        obj2.putAll(System.getenv());
        serverChannel.get().writeAndFlush(obj2).await(1000);
        assertEquals(obj2, clientQueue.poll(1000, TimeUnit.MILLISECONDS));

        SecretKey obj3 = new SecretKeySpec(new byte[]{1, 2, 3, 4}, "RAW");
        clientChannel.get().writeAndFlush(obj3).await(1000);
        assertEquals(obj3, serverQueue.poll(1000, TimeUnit.MILLISECONDS));
    }

    private void addContext(SimpleContainer container) {
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(getInstrumentation().getTargetContext()));

        SharedPreferences sharedPref = container.get(ContainerService.KEY_CONTEXT)
                .getSharedPreferences(CoreConstants.NettyConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE);
        assertTrue(
                sharedPref.edit()
                        .clear()
                        .putString(CoreConstants.NettyConstants.PREF_HOST, "localhost")
                        .commit()
        );
    }

    @NonNull
    private Server addServer(SimpleContainer container) {
        container.register(Server.KEY, new Server());

        Server server = container.get(Server.KEY);
        assertNotNull(server.getAddress());
        assertTrue(server.isChannelOpen());
        assertTrue(server.isExecutorAlive());
        return server;
    }

    private void shutdownServer(SimpleContainer container) throws InterruptedException {
        Server server = container.get(Server.KEY);
        container.shutdown();
        if (server != null) {
            server.awaitShutdown();
            assertFalse(server.isChannelOpen());
            assertFalse(server.isExecutorAlive());
        }
    }

    private static class TestServer extends Server {
        private final BlockingQueue<Object> serverQueue;
        private final Promise<SocketChannel> serverChannel;

        public TestServer(BlockingQueue<Object> serverQueue, Promise<SocketChannel> serverChannel) {
            this.serverQueue = serverQueue;
            this.serverChannel = serverChannel;
        }

        @NonNull
        @Override
        protected ServerHandshakeHandler getHandshakeHandler() {
            return new ServerHandshakeHandler(this, getContainer()) {
                @Override
                public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
                    super.channelRegistered(ctx);
                    ctx.pipeline().addAfter(LoggingHandler.class.getSimpleName(), "serverQueue",
                            new MessageToMessageDecoder<Object>() {
                                @Override
                                protected void decode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
                                    serverQueue.add(msg);
                                }
                            });
                    serverChannel.setSuccess((SocketChannel) ctx.channel());
                }
            };
        }
    }

    private static class TestClient extends Client {
        private final BlockingQueue<Object> clientQueue;
        private final Promise<SocketChannel> clientChannel;

        public TestClient(BlockingQueue<Object> clientQueue, Promise<SocketChannel> clientChannel) {
            this.clientQueue = clientQueue;
            this.clientChannel = clientChannel;
        }

        @NonNull
        @Override
        protected ClientHandshakeHandler getHandshakeHandler() {
            return new ClientHandshakeHandler(this, getContainer()) {
                @Override
                public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
                    super.channelRegistered(ctx);
                    ctx.pipeline().addAfter(LoggingHandler.class.getSimpleName(), "clientQueue",
                            new MessageToMessageDecoder<Object>() {
                                @Override
                                protected void decode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
                                    clientQueue.add(msg);
                                }
                            });
                    clientChannel.setSuccess((SocketChannel) ctx.channel());
                }
            };
        }
    }
}