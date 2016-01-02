package de.unipassau.isl.evs.ssh.master.network;

import android.util.Log;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys;
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
import de.unipassau.isl.evs.ssh.core.network.handshake.HandshakeException;
import de.unipassau.isl.evs.ssh.core.network.handshake.HandshakePacket;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;
import de.unipassau.isl.evs.ssh.master.handler.MasterRegisterDeviceHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.ALL_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.ATTR_PEER_ID;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.READER_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.WRITER_IDLE_TIME;

/**
 * @author Niko Fink: Handshake Sequence
 * @author Christoph Fraedrich Registration
 */
@ChannelHandler.Sharable
public class ServerHandshakeHandler extends ChannelHandlerAdapter {
    private static final String TAG = ServerHandshakeHandler.class.getSimpleName();

    private static final AttributeKey<byte[]> CHAP_CALLENGE = AttributeKey.valueOf(ServerHandshakeHandler.class, "CHAP_CHALLENGE");
    private static final AttributeKey<State> STATE = AttributeKey.valueOf(ServerHandshakeHandler.class, "STATE");

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

        // Timeout Handler
        ctx.pipeline().addBefore(ctx.name(), IdleStateHandler.class.getSimpleName(),
                new IdleStateHandler(READER_IDLE_TIME, WRITER_IDLE_TIME, ALL_IDLE_TIME));
        ctx.pipeline().addBefore(ctx.name(), TimeoutHandler.class.getSimpleName(), new TimeoutHandler());

        // Add exception handler
        ctx.pipeline().addLast(PipelinePlug.class.getSimpleName(), new PipelinePlug());

        super.channelRegistered(ctx);
        Log.v(TAG, "Pipeline after register: " + ctx.pipeline());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Log.v(TAG, "channelActive " + ctx);
        super.channelActive(ctx);
        assert container.require(NamingManager.KEY).isMaster();
        setState(ctx, null, State.EXPECT_HELLO);
        Log.v(TAG, "Channel open, waiting for Client Hello");
        setChapChallenge(ctx, new byte[HandshakePacket.CHAP.CHALLENGE_LENGTH]);
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof HandshakePacket.Hello) {
                handleHello(ctx, ((HandshakePacket.Hello) msg));
            } else if (msg instanceof HandshakePacket.CHAP) {
                if (getState(ctx) == State.EXPECT_INITIAL_CHAP) {
                    handleInitialChapRequest(ctx, ((HandshakePacket.CHAP) msg));
                } else {
                    handleFinalChapResponse(ctx, ((HandshakePacket.CHAP) msg));
                }
            } else if (msg instanceof HandshakePacket.ActiveRegistrationRequest) {
                handleActiveRegistrationRequest(ctx, ((HandshakePacket.ActiveRegistrationRequest) msg));
            } else {
                throw new HandshakeException("Illegal Handshake packet received");
            }
        } catch (Exception e) {
            ctx.close();
            throw e;
        }
    }

    private void handleHello(ChannelHandlerContext ctx, HandshakePacket.Hello msg) throws GeneralSecurityException {
        setState(ctx, State.EXPECT_HELLO, State.EXPECT_INITIAL_CHAP);
        Log.v(TAG, "Got Client Hello, sending Server Hello and awaiting 1. CHAP as response");

        assert !msg.isMaster;
        final X509Certificate deviceCertificate = msg.certificate;
        ctx.attr(CoreConstants.NettyConstants.ATTR_PEER_CERT).set(deviceCertificate);
        final DeviceID deviceID = DeviceID.fromCertificate(deviceCertificate);
        ctx.attr(CoreConstants.NettyConstants.ATTR_PEER_ID).set(deviceID);
        Log.v(TAG, "Client " + deviceID + " connected, checking authentication");

        final X509Certificate masterCert = container.require(NamingManager.KEY).getMasterCertificate();
        final boolean isMaster = container.require(NamingManager.KEY).isMaster();
        ctx.writeAndFlush(new HandshakePacket.Hello(masterCert, isMaster)).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

        // add Security handlers
        final PublicKey remotePublicKey = deviceCertificate.getPublicKey();
        final PrivateKey localPrivateKey = container.require(KeyStoreController.KEY).getOwnPrivateKey();
        ctx.pipeline().addBefore(ObjectEncoder.class.getSimpleName(), Encrypter.class.getSimpleName(), new Encrypter(remotePublicKey));
        ctx.pipeline().addBefore(ObjectEncoder.class.getSimpleName(), Decrypter.class.getSimpleName(), new Decrypter(localPrivateKey));
        ctx.pipeline().addBefore(ObjectEncoder.class.getSimpleName(), SignatureChecker.class.getSimpleName(), new SignatureChecker(remotePublicKey));
        ctx.pipeline().addBefore(ObjectEncoder.class.getSimpleName(), SignatureGenerator.class.getSimpleName(), new SignatureGenerator(localPrivateKey));
    }

    private void handleInitialChapRequest(ChannelHandlerContext ctx, HandshakePacket.CHAP msg) throws HandshakeException {
        setState(ctx, State.EXPECT_INITIAL_CHAP, State.EXPECT_FINAL_CHAP);
        Log.v(TAG, "Got 1. CHAP, sending 2. CHAP and awaiting 3. CHAP as response");

        if (msg.challenge == null || msg.response != null) {
            throw new HandshakeException("Illegal CHAP Response");
        }
        final byte[] chapChallenge = getChapChallenge(ctx);
        new SecureRandom().nextBytes(chapChallenge);
        ctx.writeAndFlush(new HandshakePacket.CHAP(chapChallenge, msg.challenge)).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    private void handleFinalChapResponse(ChannelHandlerContext ctx, HandshakePacket.CHAP msg) throws HandshakeException {
        setState(ctx, State.EXPECT_FINAL_CHAP, State.CHECK_AUTH);
        Log.v(TAG, "Got 3. CHAP, sending Status");

        if (msg.challenge != null || msg.response == null) {
            throw new HandshakeException("Illegal CHAP Response");
        }
        if (!Arrays.equals(getChapChallenge(ctx), msg.response)) {
            throw new HandshakeException("CHAP Packet with invalid response");
        }

        checkAuthentication(ctx);
    }

    private void checkAuthentication(ChannelHandlerContext ctx) throws HandshakeException {
        setState(ctx, State.CHECK_AUTH, State.CHECK_AUTH);

        final DeviceID deviceID = ctx.attr(CoreConstants.NettyConstants.ATTR_PEER_ID).get();
        final Slave slave = container.require(SlaveController.KEY).getSlave(deviceID);
        final UserDevice userDevice = container.require(UserManagementController.KEY).getUserDevice(deviceID);
        if (slave != null || userDevice != null) {
            setState(ctx, State.CHECK_AUTH, State.FINISHED);

            handshakeSuccessful(ctx);

            ctx.writeAndFlush(new HandshakePacket.ServerAuthenticationResponse(
                    true, null, (slave == null ? null : slave.getPassiveRegistrationToken())
            )).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        } else {
            setState(ctx, State.CHECK_AUTH, State.EXPECT_REGISTER);
            Log.i(TAG, "Device " + deviceID + " is not registered, requesting registration");

            ctx.writeAndFlush(new HandshakePacket.ServerAuthenticationResponse(
                    false, "Unknown Client, please register.", null
            )).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        }
    }

    private void handleActiveRegistrationRequest(ChannelHandlerContext ctx, HandshakePacket.ActiveRegistrationRequest msg) throws HandshakeException {
        setState(ctx, State.EXPECT_REGISTER, State.CHECK_AUTH);

        // send client register info to handler
        boolean success = container.require(MasterRegisterDeviceHandler.KEY).registerDevice(
                ctx.attr(CoreConstants.NettyConstants.ATTR_PEER_CERT).get(),
                msg.activeRegistrationToken
        );

        if (success) {
            Log.v(TAG, "Accepted registration request from " + ctx.attr(CoreConstants.NettyConstants.ATTR_PEER_ID).get());
            checkAuthentication(ctx);
        } else {
            setState(ctx, State.CHECK_AUTH, State.EXPECT_REGISTER);
            Log.v(TAG, "Rejected registration request from " + ctx.attr(CoreConstants.NettyConstants.ATTR_PEER_ID).get());

            ctx.writeAndFlush(new HandshakePacket.ServerAuthenticationResponse(
                    false, "Client registration rejected, closing connection.", null
            )).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        }
    }

    protected void handshakeSuccessful(ChannelHandlerContext ctx) {
        final State state = getState(ctx);
        if (state != State.FINISHED) {
            throw new IllegalStateException("Handshake not finished: " + state);
        }
        final DeviceID deviceID = ctx.channel().attr(ATTR_PEER_ID).get();

        // allow pings
        TimeoutHandler.setPingEnabled(ctx.channel(), true);
        // add Dispatcher
        ctx.pipeline().addBefore(ctx.name(), ClientIncomingDispatcher.class.getSimpleName(), server.getIncomingDispatcher());
        // Logging is handled by IncomingDispatcher and OutgoingRouter
        ctx.pipeline().remove(LoggingHandler.class.getSimpleName());
        // remove HandshakeHandler
        ctx.pipeline().remove(this);

        // Register connection
        server.getActiveChannels().add(ctx.channel());
        Log.i(TAG, "Handshake with " + deviceID + " successful, current Pipeline: " + ctx.pipeline());

        Message message = new Message(new DeviceConnectedPayload(deviceID, ctx.channel()));
        container.require(OutgoingRouter.KEY).sendMessageLocal(RoutingKeys.MASTER_DEVICE_CONNECTED, message);
    }

    private void setState(ChannelHandlerContext ctx, State expectedState, State newState) throws HandshakeException {
        if (!ctx.channel().attr(STATE).compareAndSet(expectedState, newState)) {
            throw new HandshakeException("Expected state " + expectedState + " but was " + getState(ctx) + ", " +
                    "new state would have been " + newState);
        }
        Log.v(TAG, "State transition " + expectedState + " -> " + newState);
    }

    private State getState(ChannelHandlerContext ctx) {
        return ctx.channel().attr(STATE).get();
    }

    private void setChapChallenge(ChannelHandlerContext ctx, byte[] value) {
        ctx.channel().attr(CHAP_CALLENGE).set(value);
    }

    private byte[] getChapChallenge(ChannelHandlerContext ctx) {
        return ctx.channel().attr(CHAP_CALLENGE).get();
    }

    private enum State {
        EXPECT_HELLO, EXPECT_INITIAL_CHAP, EXPECT_FINAL_CHAP, EXPECT_REGISTER, CHECK_AUTH, FINISHED
    }
}
