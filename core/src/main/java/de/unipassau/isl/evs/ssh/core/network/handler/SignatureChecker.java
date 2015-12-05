package de.unipassau.isl.evs.ssh.core.network.handler;

import android.util.Log;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.Security.MESSAGE_SIGN_ALG;

/**
 * The SignatureChecker class is a channel handler that is part of a ChannelPipeline and checks signatures of received messages.
 */
public class SignatureChecker extends ChannelHandlerAdapter {
    private static final String TAG = SignatureChecker.class.getSimpleName();

    private final Signature verifySignature;

    public SignatureChecker(PublicKey remotePublicKey) throws GeneralSecurityException {
        verifySignature = Signature.getInstance(MESSAGE_SIGN_ALG);
        verifySignature.initVerify(remotePublicKey);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            final ByteBuf in = (ByteBuf) msg;
            final int dataLength = in.readInt();
            final ByteBuf data = in.readSlice(dataLength);
            final int signatureLength = in.readInt();
            final byte[] signature = new byte[signatureLength];
            in.readBytes(signature);

            verifySignature.update(data.nioBuffer());
            final boolean valid = verifySignature.verify(signature);
            Log.v(TAG, "Read " + dataLength + "b of data with " + signatureLength + "b " +
                    (valid ? "valid" : "invalid") + " signature" +
                    (Log.isLoggable(TAG, Log.VERBOSE) ? ": " + Arrays.toString(signature) : ""));
            if (valid) {
                data.retain();
                ctx.fireChannelRead(data);
            } else {
                throw new SignatureException("Message has a broken signature");
            }
        } else {
            throw new SignatureException("Can't check signature of message of type " + (msg != null ? msg.getClass() : "null"));
        }
    }
}