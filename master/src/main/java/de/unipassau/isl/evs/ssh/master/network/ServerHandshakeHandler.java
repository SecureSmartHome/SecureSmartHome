package de.unipassau.isl.evs.ssh.master.network;

import android.util.Log;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DeviceConnectedPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.ClientIncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.network.handler.Decrypter;
import de.unipassau.isl.evs.ssh.core.network.handler.Encrypter;
import de.unipassau.isl.evs.ssh.core.network.handler.PipelinePlug;
import de.unipassau.isl.evs.ssh.core.network.handler.SignatureChecker;
import de.unipassau.isl.evs.ssh.core.network.handler.SignatureGenerator;
import de.unipassau.isl.evs.ssh.core.network.handler.TimeoutHandler;
import de.unipassau.isl.evs.ssh.core.network.handshake.HandshakePacket;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import de.unipassau.isl.evs.ssh.master.handler.MasterRegisterDeviceHandler;
import io.netty.channel.ChannelHandler;
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

@ChannelHandler.Sharable
public class ServerHandshakeHandler extends ChannelHandlerAdapter {
    private static final String TAG = ServerHandshakeHandler.class.getSimpleName();

    private final Server server;
    private final Container container;

    public ServerHandshakeHandler(Server server, Container container) {
        this.server = server;
        this.container = container;
    }

    /**
     * Configures the per-connection pipeline that is responsible for handling incoming and outgoing data.
     * After an incoming packet is decrypted, decoded and verified,
     * it will be sent to its target {@link MessageHandler}
     * by the {@link ClientIncomingDispatcher}.
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Log.v(TAG, "channelRegistered " + ctx);
        if (container == null) {
            //Do not accept new connections after the Server has been shut down
            Log.v(TAG, "channelRegistered:closed");
            ctx.close();
            return;
        }

        // Add (de-)serialization Handlers before this Handler
        ctx.pipeline().addBefore(ctx.name(), ObjectEncoder.class.getSimpleName(), new ObjectEncoder());
        ctx.pipeline().addBefore(ctx.name(), ObjectDecoder.class.getSimpleName(), new ObjectDecoder(
                ClassResolvers.weakCachingConcurrentResolver(getClass().getClassLoader())));
        ctx.pipeline().addBefore(ctx.name(), LoggingHandler.class.getSimpleName(), new LoggingHandler(LogLevel.TRACE));

        // Add exception handler
        ctx.pipeline().addLast(PipelinePlug.class.getSimpleName(), new PipelinePlug());

        // Register connection
        server.getActiveChannels().add(ctx.channel());

        super.channelRegistered(ctx);
        Log.v(TAG, "Pipeline after register: " + ctx.pipeline());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Log.v(TAG, "channelActive " + ctx);

        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HandshakePacket) {
            Log.v(TAG, "Received " + msg);
            if (msg instanceof HandshakePacket.ClientRegistration) {
                //Send client register info to handler
                HandshakePacket.ClientRegistration clientRegistration = ((HandshakePacket.ClientRegistration) msg);
                container.require(MasterRegisterDeviceHandler.KEY).registerDevice(
                        clientRegistration.clientCertificate,
                        clientRegistration.token
                );

            } else if (msg instanceof HandshakePacket.ClientHello) {
                final HandshakePacket.ClientHello hello = (HandshakePacket.ClientHello) msg;
                ctx.attr(CoreConstants.NettyConstants.ATTR_PEER_CERT).set(hello.clientCertificate);
                final DeviceID deviceID = DeviceID.fromCertificate(hello.clientCertificate);
                ctx.attr(CoreConstants.NettyConstants.ATTR_PEER_ID).set(deviceID);
                Log.i(TAG, "Client " + deviceID + " connected");

                final X509Certificate masterCert = container.require(NamingManager.KEY).getMasterCertificate();
                ctx.writeAndFlush(new HandshakePacket.ServerHello(masterCert, null));

                //TODO check authentication and protocol version and close connection on fail
                clientAuthenticated(ctx, hello.clientCertificate, deviceID);
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }

    /**
     * Called once the Handshake is complete
     */
    private void clientAuthenticated(ChannelHandlerContext ctx, X509Certificate clientCertificate, DeviceID deviceID) {
        Log.v(TAG, "clientAuthenticated " + ctx);

        //Security
        final PublicKey remotePublicKey = clientCertificate.getPublicKey();
        final PrivateKey localPrivateKey = container.require(KeyStoreController.KEY).getOwnPrivateKey();
        try {
            ctx.pipeline().addBefore(ObjectEncoder.class.getSimpleName(), Encrypter.class.getSimpleName(), new Encrypter(remotePublicKey));
            ctx.pipeline().addBefore(ObjectEncoder.class.getSimpleName(), Decrypter.class.getSimpleName(), new Decrypter(localPrivateKey));
            ctx.pipeline().addBefore(ObjectEncoder.class.getSimpleName(), SignatureChecker.class.getSimpleName(), new SignatureChecker(remotePublicKey));
            ctx.pipeline().addBefore(ObjectEncoder.class.getSimpleName(), SignatureGenerator.class.getSimpleName(), new SignatureGenerator(localPrivateKey));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Could not set up Security Handlers", e);//TODO handle
        }

        //Timeout Handler
        ctx.pipeline().addBefore(ctx.name(), IdleStateHandler.class.getSimpleName(),
                new IdleStateHandler(CLIENT_READER_IDLE_TIME, CLIENT_WRITER_IDLE_TIME, CLIENT_ALL_IDLE_TIME));
        ctx.pipeline().addBefore(ctx.name(), TimeoutHandler.class.getSimpleName(), new TimeoutHandler());

        //Dispatcher
        ctx.pipeline().addBefore(ctx.name(), ServerIncomingDispatcher.class.getSimpleName(), server.getIncomingDispatcher());

        ctx.pipeline().remove(this);
        Log.v(TAG, "Pipeline after authenticate: " + ctx.pipeline());

        Message message = new Message(new DeviceConnectedPayload(deviceID, ctx.channel()));
        container.require(OutgoingRouter.KEY).sendMessageLocal(CoreConstants.RoutingKeys.MASTER_DEVICE_CONNECTED, message);
    }


    private void handshakeClientRegisters(ChannelHandlerContext ctx, Object msg) {
        HandshakePacket.ClientRegistration clientReg = ((HandshakePacket.ClientRegistration) msg);
        //DO Stuff
    }

}
