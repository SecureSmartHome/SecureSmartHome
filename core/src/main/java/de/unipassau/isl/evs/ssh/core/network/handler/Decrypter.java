package de.unipassau.isl.evs.ssh.core.network.handler;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.List;

import javax.crypto.Cipher;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.Security.MESSAGE_CRYPT_ALG;

/**
 * The Decrypter class is a channel handler that is part of a ChannelPipeline and provides decryption for system messages.
 */
public class Decrypter extends ReplayingDecoder {
    private final Cipher decryptCipher;

    public Decrypter(PrivateKey localPrivateKey) throws GeneralSecurityException {
        decryptCipher = Cipher.getInstance(MESSAGE_CRYPT_ALG);
        decryptCipher.init(Cipher.DECRYPT_MODE, localPrivateKey);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        final int encryptedLength = in.readInt();
        final int decryptedLength = decryptCipher.getOutputSize(encryptedLength);

        final ByteBuffer inBuffer = in.nioBuffer(in.readerIndex(), encryptedLength);
        final ByteBuf outBuffer = ctx.alloc().buffer(decryptedLength);
        decryptCipher.doFinal(inBuffer, outBuffer.nioBuffer());
        outBuffer.writerIndex(outBuffer.writerIndex() + decryptedLength);
        in.readerIndex(in.readerIndex() + encryptedLength);

        out.add(outBuffer);
    }
}