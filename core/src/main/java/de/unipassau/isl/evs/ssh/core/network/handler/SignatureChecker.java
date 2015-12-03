package de.unipassau.isl.evs.ssh.core.network.handler;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.Security.MESSAGE_SIGN_ALG;

/**
 * The SignatureChecker class is a channel handler that is part of a ChannelPipeline and checks signatures of received messages.
 */
public class SignatureChecker extends ReplayingDecoder {
    private final Signature verifySignature;

    public SignatureChecker(PublicKey remotePublicKey) throws GeneralSecurityException {
        verifySignature = Signature.getInstance(MESSAGE_SIGN_ALG);
        verifySignature.initVerify(remotePublicKey);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        final int dataLength = in.readInt();
        final ByteBuf data = in.readSlice(dataLength);

        verifySignature.update(data.nioBuffer());

        final int signatureLength = in.readInt();
        final byte[] signature = new byte[signatureLength];
        in.readBytes(signature);

        if (verifySignature.verify(signature)) {
            out.add(data);
        } else {
            throw new SignatureException("Message has a broken signature");
        }
    }
}