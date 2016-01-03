package de.unipassau.isl.evs.ssh.core.network.handler;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * The SignatureGenerator class is a channel handler that is part of a ChannelPipeline and signs messages.
 *
 * @author Niko Fink
 */
public class SignatureGenerator extends MessageToByteEncoder<ByteBuf> {
    private static final String TAG = SignatureGenerator.class.getSimpleName();
    static final String MESSAGE_SIGN_ALG = "SHA224withECDSA";

    private final Signature signSignature;

    public SignatureGenerator(PrivateKey localPrivateKey) throws GeneralSecurityException {
        signSignature = Signature.getInstance(MESSAGE_SIGN_ALG);
        signSignature.initSign(localPrivateKey);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        final int dataLength = msg.readableBytes();
        msg.markReaderIndex();
        out.writeInt(dataLength);
        out.writeBytes(msg);
        msg.resetReaderIndex();

        signSignature.update(msg.nioBuffer());
        msg.readerIndex(msg.writerIndex());

        final byte[] signature = signSignature.sign();
        final int signatureLength = signature.length;
        out.writeInt(signatureLength);
        out.writeBytes(signature);

        //Log.v(TAG, "Signed " + dataLength + "b of data with " + signatureLength + "b signature" +
        //        (Log.isLoggable(TAG, Log.VERBOSE) ? ": " + Arrays.toString(signature) : ""));
    }
}