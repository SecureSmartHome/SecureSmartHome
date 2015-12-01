package de.unipassau.isl.evs.ssh.core.network;

import android.util.Log;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.handler.PipelinePlug;
import de.unipassau.isl.evs.ssh.core.network.handler.TimeoutHandler;
import de.unipassau.isl.evs.ssh.core.network.handshake.HandshakePacket;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.CLIENT_ALL_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.CLIENT_READER_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.CLIENT_WRITER_IDLE_TIME;

public class ClientHandshakeHandler extends ChannelHandlerAdapter {
    private static final String TAG = ClientHandshakeHandler.class.getSimpleName();

    private final Client client;
    private final Container container;

    public ClientHandshakeHandler(Client client, Container container) {
        this.client = client;
        this.container = container;
    }

    /**
     * Called once the TCP connection is established.
     * Configures the per-connection pipeline that is responsible for handling incoming and outgoing data.
     * After an incoming packet is decrypted, decoded and verified,
     * it will be sent to its target {@link de.unipassau.isl.evs.ssh.core.handler.MessageHandler}
     * by the {@link ClientIncomingDispatcher}.
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Log.v(TAG, "channelRegistered " + ctx);

        // Add (de-)serialization Handlers before this Handler
        ctx.pipeline().addBefore(ctx.name(), ObjectEncoder.class.getSimpleName(), new ObjectEncoder());
        ctx.pipeline().addBefore(ctx.name(), ObjectDecoder.class.getSimpleName(), new ObjectDecoder(
                ClassResolvers.weakCachingConcurrentResolver(getClass().getClassLoader())));
        ctx.pipeline().addBefore(ctx.name(), LoggingHandler.class.getSimpleName(), new LoggingHandler(LogLevel.TRACE));

        // Add exception handler
        ctx.pipeline().addAfter(ctx.name(), PipelinePlug.class.getSimpleName(), new PipelinePlug());

        super.channelRegistered(ctx);
        Log.v(TAG, "Pipeline after register: " + ctx.pipeline());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Log.v(TAG, "channelActive " + ctx);

        final NamingManager namingManager = container.require(NamingManager.KEY);
        ctx.writeAndFlush(new HandshakePacket.ClientHello(namingManager.getOwnCertificate(), namingManager.getMasterCertificate()));

        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HandshakePacket.ServerHello) {
            handshakeComplete(ctx);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    /**
     * Called once the Handshake is complete
     */
    private void handshakeComplete(ChannelHandlerContext ctx) {
        Log.v(TAG, "handshakeComplete " + ctx);

        //Timeout Handler
        ctx.pipeline().addBefore(ctx.name(), IdleStateHandler.class.getSimpleName(),
                new IdleStateHandler(CLIENT_READER_IDLE_TIME, CLIENT_WRITER_IDLE_TIME, CLIENT_ALL_IDLE_TIME));
        ctx.pipeline().addBefore(ctx.name(), TimeoutHandler.class.getSimpleName(), new TimeoutHandler());

        //Dispatcher
        ctx.pipeline().addBefore(ctx.name(), ClientIncomingDispatcher.class.getSimpleName(), client.getIncomingDispatcher());

        ctx.pipeline().remove(this);
        Log.v(TAG, "Pipeline after handshake: " + ctx.pipeline());
    }
}
