package de.unipassau.isl.evs.ssh.core.network.handler;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

import javax.crypto.Cipher;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.Security.MESSAGE_CRYPT_ALG;

/**
 * The Encrypter class is a channel handler that Is part of a ChannelPipeline and provides encryption for system messages.
 */
public class Encrypter extends MessageToByteEncoder<ByteBuf> {
    private final Cipher encryptCipher;

    public Encrypter(PublicKey remotePublicKey) throws GeneralSecurityException {
        encryptCipher = Cipher.getInstance(MESSAGE_CRYPT_ALG);
        encryptCipher.init(Cipher.ENCRYPT_MODE, remotePublicKey);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        final int decryptedLength = msg.readableBytes();
        final int encryptedLength = encryptCipher.getOutputSize(decryptedLength);
        out.writeInt(encryptedLength);

        out.ensureWritable(encryptedLength);
        encryptCipher.doFinal(msg.nioBuffer(), out.nioBuffer());
        out.writerIndex(out.writerIndex() + encryptedLength);
        msg.readerIndex(msg.readerIndex() + decryptedLength);
    }
}