package de.unipassau.isl.evs.ssh.core.network;

import android.util.Log;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.handler.Decrypter;
import de.unipassau.isl.evs.ssh.core.network.handler.Encrypter;
import de.unipassau.isl.evs.ssh.core.network.handler.PipelinePlug;
import de.unipassau.isl.evs.ssh.core.network.handler.SignatureChecker;
import de.unipassau.isl.evs.ssh.core.network.handler.SignatureGenerator;
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
            //TODO check authentication and protocol version and close connection on fail
            handshakeComplete(ctx, ((HandshakePacket.ServerHello) msg).serverCertificate);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    /**
     * Called once the Handshake is complete
     */
    private void handshakeComplete(ChannelHandlerContext ctx, X509Certificate serverCertificate) {
        Log.v(TAG, "handshakeComplete " + ctx);

        //Security
        final PublicKey remotePublicKey = serverCertificate.getPublicKey();
        final PrivateKey localPrivateKey = container.require(KeyStoreController.KEY).getOwnPrivateKey();
        try {
            ctx.pipeline().addBefore(ctx.name(), Encrypter.class.getSimpleName(), new Encrypter(remotePublicKey));
            ctx.pipeline().addBefore(ctx.name(), Decrypter.class.getSimpleName(), new Decrypter(localPrivateKey));
            ctx.pipeline().addBefore(ctx.name(), SignatureChecker.class.getSimpleName(), new SignatureChecker(remotePublicKey));
            ctx.pipeline().addBefore(ctx.name(), SignatureGenerator.class.getSimpleName(), new SignatureGenerator(localPrivateKey));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Could not set up Security Handlers", e);//TODO handle
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
