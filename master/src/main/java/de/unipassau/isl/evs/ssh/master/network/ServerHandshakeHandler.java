package de.unipassau.isl.evs.ssh.master.network;

import android.util.Log;

import java.security.cert.X509Certificate;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.FinalizeRegisterUserDevicePayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.ClientIncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.network.handler.PipelinePlug;
import de.unipassau.isl.evs.ssh.core.network.handler.TimeoutHandler;
import de.unipassau.isl.evs.ssh.core.network.handshake.HandshakePacket;
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

    public ServerHandshakeHandler(Server server) {
        this.server = server;
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
        if (getContainer() == null) {
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
        ctx.pipeline().addAfter(ctx.name(), PipelinePlug.class.getSimpleName(), new PipelinePlug());

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
        if (msg instanceof HandshakePacket.ClientRegistration) {
            //Send client register info to handler
            HandshakePacket.ClientRegistration clientRegistration = ((HandshakePacket.ClientRegistration) msg);
            Message message = new Message(new FinalizeRegisterUserDevicePayload(
                    clientRegistration.token, clientRegistration.clientCertificate
            ));
            getContainer().require(OutgoingRouter.KEY).sendMessageLocal(CoreConstants.RoutingKeys.MASTER_REGISTER_FINALIZE, message);

        } else if (msg instanceof HandshakePacket.ClientHello) {
            final HandshakePacket.ClientHello hello = (HandshakePacket.ClientHello) msg;
            ctx.attr(CoreConstants.NettyConstants.ATTR_CLIENT_CERT).set(hello.clientCertificate);
            final DeviceID deviceID = DeviceID.fromCertificate(hello.clientCertificate);
            ctx.attr(CoreConstants.NettyConstants.ATTR_CLIENT_ID).set(deviceID);
            Log.i(TAG, "Client " + deviceID + " connected");

            final X509Certificate masterCert = getContainer().require(NamingManager.KEY).getMasterCertificate();
            ctx.writeAndFlush(new HandshakePacket.ServerHello(masterCert));

            //TODO check authentication and protocol version and close connection on fail
            clientAuthenticated(ctx);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    /**
     * Called once the Handshake is complete
     */
    private void clientAuthenticated(ChannelHandlerContext ctx) {
        Log.v(TAG, "clientAuthenticated " + ctx);

        //Timeout Handler
        ctx.pipeline().addBefore(ctx.name(), IdleStateHandler.class.getSimpleName(),
                new IdleStateHandler(CLIENT_READER_IDLE_TIME, CLIENT_WRITER_IDLE_TIME, CLIENT_ALL_IDLE_TIME));
        ctx.pipeline().addBefore(ctx.name(), TimeoutHandler.class.getSimpleName(), new TimeoutHandler());

        //Dispatcher
        ctx.pipeline().addBefore(ctx.name(), ServerIncomingDispatcher.class.getSimpleName(), server.getIncomingDispatcher());

        ctx.pipeline().remove(this);
        Log.v(TAG, "Pipeline after authenticate: " + ctx.pipeline());
    }


    private void handshakeClientRegisters(ChannelHandlerContext ctx, Object msg) {
        HandshakePacket.ClientRegistration clientReg = ((HandshakePacket.ClientRegistration) msg);
        //DO Stuff
    }


    protected Container getContainer() {
        return server._getContainer();
    }
}
