package de.unipassau.isl.evs.ssh.core.network;

import android.util.Base64;
import android.util.Log;

import com.google.common.base.Strings;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import de.unipassau.isl.evs.ssh.core.container.Container;
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
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.ATTR_PEER_CERT;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.ATTR_PEER_ID;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.CLIENT_ALL_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.CLIENT_READER_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.CLIENT_WRITER_IDLE_TIME;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.SharedPrefs.PREF_TOKEN;

/**
 * @author Niko Fink: Handshake Sequence
 * @author Christoph Fraedrich: Registration
 */
public class ClientHandshakeHandler extends ChannelHandlerAdapter {
    private static final String TAG = ClientHandshakeHandler.class.getSimpleName();

    private final Client client;
    private final Container container;
    private byte[] chapChallenge = new byte[HandshakePacket.CHAP.CHALLENGE_LENGTH];
    private State state;
    private boolean triedRegister;

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

        // Timeout Handler
        ctx.pipeline().addBefore(ctx.name(), IdleStateHandler.class.getSimpleName(),
                new IdleStateHandler(CLIENT_READER_IDLE_TIME, CLIENT_WRITER_IDLE_TIME, CLIENT_ALL_IDLE_TIME));
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

    private void handleHello(ChannelHandlerContext ctx, HandshakePacket.Hello msg) throws GeneralSecurityException {
        setState(State.EXPECT_HELLO, State.EXPECT_CHAP);
        Log.v(TAG, "Got Server Hello, sending 1. CHAP and awaiting 2. CHAP as response");

        // import data from Hello packet
        assert msg.isMaster;
        final NamingManager namingManager = container.require(NamingManager.KEY);
        final DeviceID masterID = namingManager.getMasterID();
        final DeviceID certID = DeviceID.fromCertificate(msg.certificate);
        if (!masterID.equals(certID)) {
            throw new HandshakeException("Server DeviceID " + certID + " did not match my MasterID " + masterID);
        }
        if (!namingManager.isMasterKnown()) {
            // first connection to master, register certificate for already known DeviceID
            namingManager.setMasterCertificate(msg.certificate);
        }
        if (!namingManager.isMasterKnown()) {
            throw new HandshakeException("Received Hello from a Master, but could not register that Master as mine");
        }
        final X509Certificate masterCertificate = namingManager.getMasterCertificate();

        // set channel attributes
        ctx.attr(ATTR_PEER_CERT).set(masterCertificate);
        ctx.attr(ATTR_PEER_ID).set(masterID);

        // add Security handlers
        final PublicKey remotePublicKey = masterCertificate.getPublicKey();
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

    private void handleServerAuthenticationResponse(ChannelHandlerContext ctx, HandshakePacket.ServerAuthenticationResponse msg) throws HandshakeException {
        setState(State.EXPECT_STATE, State.STATE_RECEIVED);

        if (msg.isAuthenticated) {
            setState(State.STATE_RECEIVED, State.FINISHED);
            Log.v(TAG, "Got State: authenticated, handshake successful");
            handshakeSuccessful(ctx);
        } else {
            final String tokenString = client.getSharedPrefs().getString(PREF_TOKEN, null);
            final boolean canRegister = !triedRegister && !Strings.isNullOrEmpty(tokenString);
            if (canRegister) {
                triedRegister = true;
                setState(State.STATE_RECEIVED, State.EXPECT_STATE);
                Log.v(TAG, "Got State: unauthenticated, trying Registration");
                final byte[] token = Base64.decode(tokenString, Base64.NO_WRAP);

                ctx.writeAndFlush(new HandshakePacket.RegistrationRequest(token)).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            } else {
                setState(State.STATE_RECEIVED, State.FAILED);
                Log.v(TAG, "Got State: unauthenticated and Registration is not possible, handshake failed");
                handshakeFailed(ctx, msg.message);
            }
        }
    }

    protected void handshakeSuccessful(ChannelHandlerContext ctx) {
        if (state != State.FINISHED) {
            throw new IllegalStateException("Handshake not finished: " + state);
        }

        // allow pings
        ctx.channel().attr(TimeoutHandler.SEND_PINGS).set(true);

        // add Dispatcher
        ctx.pipeline().addBefore(ctx.name(), ClientIncomingDispatcher.class.getSimpleName(), client.getIncomingDispatcher());

        ctx.pipeline().remove(this);
        Log.v(TAG, "Handshake successful, current Pipeline: " + ctx.pipeline());
    }

    protected void handshakeFailed(ChannelHandlerContext ctx, String message) {
        if (state != State.FAILED) {
            throw new IllegalStateException("Handshake not finished: " + state);
        }

        Log.e(TAG, "My Master rejected me, did he loose his mind? His message was: " + message);
        ctx.close();
        //TODO show error dialog
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
