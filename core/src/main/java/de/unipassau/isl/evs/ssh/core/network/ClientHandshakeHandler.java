package de.unipassau.isl.evs.ssh.core.network;

import android.util.Log;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.handler.PipelinePlug;
import de.unipassau.isl.evs.ssh.core.network.handler.TimeoutHandler;
import de.unipassau.isl.evs.ssh.core.network.handshake.HandshakePacket;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.CLIENT_ALL_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.CLIENT_READER_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.CLIENT_WRITER_IDLE_TIME;

public class ClientHandshakeHandler extends ChannelHandlerAdapter {
    private static final String TAG = ClientHandshakeHandler.class.getSimpleName();

    private final Client client;
    private final Container container;
    private final byte[] token;

    public ClientHandshakeHandler(Client client, Container container, byte[] token) {
        this.client = client;
        this.container = container;
        this.token = token;
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
        if (!namingManager.isMasterKnown()) {
            ctx.writeAndFlush(new HandshakePacket.ClientRegistration(namingManager.getOwnCertificate(), token));

        } else {
            ctx.writeAndFlush(new HandshakePacket.ClientHello(namingManager.getOwnCertificate(), namingManager.getMasterID()));
        }

        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HandshakePacket.ServerHello) {
            handshakeComplete(ctx, msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    /**
     * Called once the Handshake is complete
     */
    protected void handshakeComplete(ChannelHandlerContext ctx, Object msg) throws GeneralSecurityException {
        Log.v(TAG, "handshakeComplete " + ctx);

        X509Certificate cert =  ((HandshakePacket.ServerHello) msg).serverCertificate;
        KeyStoreController keyStoreController = container.require(KeyStoreController.KEY);
        if (keyStoreController.listEntries().contains(cert)) {
            DeviceID alias = DeviceID.fromCertificate(cert);
            keyStoreController.saveCertificate(cert, alias.getIDString());
        }

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
