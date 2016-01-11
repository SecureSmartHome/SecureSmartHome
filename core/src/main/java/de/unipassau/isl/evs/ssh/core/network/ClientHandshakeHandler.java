package de.unipassau.isl.evs.ssh.core.network;

import android.annotation.SuppressLint;
import android.util.Log;

import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.Arrays;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.handler.Decrypter;
import de.unipassau.isl.evs.ssh.core.network.handler.Encrypter;
import de.unipassau.isl.evs.ssh.core.network.handler.PipelinePlug;
import de.unipassau.isl.evs.ssh.core.network.handler.SignatureChecker;
import de.unipassau.isl.evs.ssh.core.network.handler.SignatureGenerator;
import de.unipassau.isl.evs.ssh.core.network.handler.TimeoutHandler;
import de.unipassau.isl.evs.ssh.core.network.handshake.HandshakeException;
import de.unipassau.isl.evs.ssh.core.network.handshake.HandshakePacket;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.ALL_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.ATTR_HANDSHAKE_FINISHED;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.ATTR_LOCAL_CONNECTION;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.ATTR_PEER_CERT;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.ATTR_PEER_ID;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.READER_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.WRITER_IDLE_TIME;

/**
 * A ChannelHandlerAdapter that will execute the Handshake with the Master and add the IncomingDispatcher on success.
 *
 * @author Niko Fink: Handshake Sequence
 * @author Christoph Fraedrich: Registration
 */
@SuppressLint("Assert")
public class ClientHandshakeHandler extends ChannelHandlerAdapter {
    private static final String TAG = ClientHandshakeHandler.class.getSimpleName();

    private final Container container;
    private final byte[] chapChallenge = new byte[HandshakePacket.CHAP.CHALLENGE_LENGTH];
    private State state;
    private boolean triedRegister;

    public ClientHandshakeHandler(Container container) {
        this.container = container;
    }

    /**
     * Called once the TCP connection is established.
     * Configures the per-connection pipeline that is responsible for handling incoming and outgoing data.
     * After an incoming packet is decrypted, decoded and verified,
     * it will be sent to its target {@link de.unipassau.isl.evs.ssh.core.handler.MessageHandler}
     * by the {@link IncomingDispatcher}.
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Log.v(TAG, "channelRegistered " + ctx);
        ctx.attr(ATTR_HANDSHAKE_FINISHED).set(false);

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
        ctx.pipeline().addAfter(ctx.name(), PipelinePlug.class.getSimpleName(), new PipelinePlug());

        super.channelRegistered(ctx);
        Log.v(TAG, "Pipeline after register: " + ctx.pipeline());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Log.v(TAG, "channelActive " + ctx);
        super.channelActive(ctx);
        final NamingManager namingManager = container.require(NamingManager.KEY);
        assert !namingManager.isMaster();
        ctx.writeAndFlush(new HandshakePacket.Hello(namingManager.getOwnCertificate(), false));
        setState(null, State.EXPECT_HELLO);
        Log.v(TAG, "Sent Client Hello, expecting Server Hello");
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof HandshakePacket.Hello) {
                handleHello(ctx, ((HandshakePacket.Hello) msg));
            } else if (msg instanceof HandshakePacket.CHAP) {
                handleChapResponse(ctx, ((HandshakePacket.CHAP) msg));
            } else if (msg instanceof HandshakePacket.ServerAuthenticationResponse) {
                handleServerAuthenticationResponse(ctx, ((HandshakePacket.ServerAuthenticationResponse) msg));
            } else {
                throw new HandshakeException("Illegal Handshake packet received");
            }
        } catch (Exception e) {
            ctx.close();
            throw e;
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Log.w(TAG, "Can't write data while authentication is pending, message was " + msg);
        ReferenceCountUtil.release(msg);
    }

    private void handleHello(ChannelHandlerContext ctx, HandshakePacket.Hello msg) throws GeneralSecurityException {
        setState(State.EXPECT_HELLO, State.EXPECT_CHAP);
        Log.v(TAG, "Got Server Hello, sending 1. CHAP and awaiting 2. CHAP as response");
        assert msg.isMaster;

        // import data from Hello packet
        final NamingManager namingManager = container.require(NamingManager.KEY);
        final DeviceID certID = DeviceID.fromCertificate(msg.certificate);
        // verify the data if the Master is already known, otherwise the registration token will be checked later
        if (namingManager.isMasterIDKnown()) {
            final DeviceID masterID = namingManager.getMasterID();
            if (!masterID.equals(certID)) {
                throw new HandshakeException("Server DeviceID " + certID + " did not match my MasterID " + masterID);
            }
            if (!namingManager.isMasterKnown()) {
                // first connection to master, register certificate for already known DeviceID
                namingManager.setMasterCertificate(msg.certificate);
            }
        }

        // set channel attributes
        ctx.attr(ATTR_PEER_CERT).set(msg.certificate);
        ctx.attr(ATTR_PEER_ID).set(certID);

        // add Security handlers
        final PublicKey remotePublicKey = msg.certificate.getPublicKey();
        final PrivateKey localPrivateKey = container.require(KeyStoreController.KEY).getOwnPrivateKey();
        ctx.pipeline().addBefore(ObjectEncoder.class.getSimpleName(), Encrypter.class.getSimpleName(), new Encrypter(remotePublicKey));
        ctx.pipeline().addBefore(ObjectEncoder.class.getSimpleName(), Decrypter.class.getSimpleName(), new Decrypter(localPrivateKey));
        ctx.pipeline().addBefore(ObjectEncoder.class.getSimpleName(), SignatureChecker.class.getSimpleName(), new SignatureChecker(remotePublicKey));
        ctx.pipeline().addBefore(ObjectEncoder.class.getSimpleName(), SignatureGenerator.class.getSimpleName(), new SignatureGenerator(localPrivateKey));

        // and send the initial CHAP packet to the master
        new SecureRandom().nextBytes(chapChallenge);
        ctx.writeAndFlush(new HandshakePacket.CHAP(chapChallenge, null)).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    private void handleChapResponse(ChannelHandlerContext ctx, HandshakePacket.CHAP msg) throws HandshakeException {
        setState(State.EXPECT_CHAP, State.EXPECT_STATE);
        Log.v(TAG, "Got 2. CHAP, sending 3. CHAP and awaiting Status as response");

        if (msg.challenge == null || msg.response == null) {
            throw new HandshakeException("Illegal CHAP Response");
        }
        if (!Arrays.equals(chapChallenge, msg.response)) {
            throw new HandshakeException("CHAP Packet with invalid response");
        }
        ctx.writeAndFlush(new HandshakePacket.CHAP(null, msg.challenge)).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    private void handleServerAuthenticationResponse(ChannelHandlerContext ctx, HandshakePacket.ServerAuthenticationResponse msg)
            throws HandshakeException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        setState(State.EXPECT_STATE, State.STATE_RECEIVED);

        final NamingManager namingManager = container.require(NamingManager.KEY);
        if (msg.isAuthenticated) {
            if (!namingManager.isMasterIDKnown()) {
                Log.i(TAG, "Got State: authenticated from unverified Master, checking token");
                if (checkSentPassiveRegistrationToken(msg)) {
                    Log.i(TAG, "Master sent correct token, saving MasterID and Certificate");
                    namingManager.setMasterCertificate(ctx.attr(ATTR_PEER_CERT).get());
                } else {
                    setState(State.STATE_RECEIVED, State.FAILED);
                    handshakeFailed(ctx, "Master is not verified yet sent invalid token");
                    return;
                }
            }
            if (!namingManager.isMasterKnown()) {
                setState(State.STATE_RECEIVED, State.FAILED);
                handshakeFailed(ctx, "Master did not authenticate itself (and is not known yet)");
                return;
            }

            setState(State.STATE_RECEIVED, State.FINISHED);
            ctx.attr(ATTR_HANDSHAKE_FINISHED).set(true);
            ctx.attr(ATTR_LOCAL_CONNECTION).set(msg.isConnectionLocal);
            Log.i(TAG, "Got State: authenticated, handshake successful");
            handshakeSuccessful(ctx);
        } else {
            final byte[] token = container.require(Client.KEY).getActiveRegistrationTokenBytes();
            final boolean canRegister = !triedRegister && token.length == DeviceConnectInformation.TOKEN_LENGTH;
            if (canRegister) {
                triedRegister = true;
                setState(State.STATE_RECEIVED, State.EXPECT_STATE);
                Log.i(TAG, "Got State: unauthenticated, trying Registration");

                ctx.writeAndFlush(new HandshakePacket.ActiveRegistrationRequest(token)).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            } else {
                setState(State.STATE_RECEIVED, State.FAILED);
                Log.w(TAG, "Got State: unauthenticated and registration is not possible, handshake failed");
                handshakeFailed(ctx, msg.message);
            }
        }
    }

    private boolean checkSentPassiveRegistrationToken(HandshakePacket.ServerAuthenticationResponse msg) {
        final byte[] expectedToken = container.require(Client.KEY).getPassiveRegistrationTokenBytes();
        final byte[] actualToken = msg.passiveRegistrationToken;
        return actualToken != null && expectedToken != null
                && actualToken.length == DeviceConnectInformation.TOKEN_LENGTH
                && Arrays.equals(actualToken, expectedToken);
    }

    protected void handshakeSuccessful(ChannelHandlerContext ctx) {
        if (state != State.FINISHED) {
            throw new IllegalStateException("Handshake not finished: " + state);
        }

        // allow pings
        TimeoutHandler.setPingEnabled(ctx.channel(), true);
        // add Dispatcher
        ctx.pipeline().addBefore(ctx.name(), IncomingDispatcher.class.getSimpleName(), container.require(IncomingDispatcher.KEY));
        // Logging is handled by IncomingDispatcher and OutgoingRouter
        ctx.pipeline().remove(LoggingHandler.class.getSimpleName());
        // remove HandshakeHandler
        ctx.pipeline().remove(this);

        Log.v(TAG, "Handshake successful, current Pipeline: " + ctx.pipeline());
        container.require(Client.KEY).notifyClientConnected();
    }

    protected void handshakeFailed(ChannelHandlerContext ctx, String message) {
        if (state != State.FAILED) {
            throw new IllegalStateException("Handshake not finished: " + state);
        }

        Log.e(TAG, "Could not establish secure connection to master: " + message);
        container.require(Client.KEY).notifyClientRejected(message);
        ctx.close();
    }

    private void setState(State expectedState, State newState) throws HandshakeException {
        if (state != expectedState) {
            throw new HandshakeException("Expected state " + expectedState + " but was " + state + ", " +
                    "new state would have been " + newState);
        }
        state = newState;
        Log.v(TAG, "State transition " + expectedState + " -> " + newState);
    }

    private enum State {
        EXPECT_HELLO, EXPECT_CHAP, EXPECT_STATE, STATE_RECEIVED, FINISHED, FAILED
    }
}
