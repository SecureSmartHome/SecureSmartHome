package de.unipassau.isl.evs.ssh.core.network.handler;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.Security.MESSAGE_SIGN_ALG;

/**
 * The SignatureGenerator class is a channel handler that is part of a ChannelPipeline and signs messages.
 */
public class SignatureGenerator extends MessageToByteEncoder<ByteBuf> {
    private final Signature signSignature;

    public SignatureGenerator(PrivateKey localPrivateKey) throws GeneralSecurityException {
        signSignature = Signature.getInstance(MESSAGE_SIGN_ALG);
        signSignature.initSign(localPrivateKey);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        msg.markReaderIndex();
        out.writeInt(msg.readableBytes());
        out.writeBytes(msg);
        msg.resetReaderIndex();

        signSignature.update(msg.nioBuffer());
        msg.readerIndex(msg.writerIndex());

        final byte[] signature = signSignature.sign();
        out.writeInt(signature.length);
        out.writeBytes(signature);
    }
}