package de.unipassau.isl.evs.ssh.core.network.handler;

import android.util.Log;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.List;

import javax.crypto.Cipher;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import static de.unipassau.isl.evs.ssh.core.network.handler.Encrypter.MESSAGE_CRYPT_ALG;

/**
 * The Decrypter class is a channel handler that is part of a ChannelPipeline and provides decryption for system messages.
 */
public class Decrypter extends ReplayingDecoder {
    private static final String TAG = Decrypter.class.getSimpleName();

    private final Cipher decryptCipher;

    public Decrypter(PrivateKey localPrivateKey) throws GeneralSecurityException {
        decryptCipher = Cipher.getInstance(MESSAGE_CRYPT_ALG);
        decryptCipher.init(Cipher.DECRYPT_MODE, localPrivateKey);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> decoded) throws Exception {
        try {
            final int encryptedLength = in.readInt();
            final int decryptedLength = decryptCipher.getOutputSize(encryptedLength);

            final ByteBuf out = ctx.alloc().buffer(decryptedLength);
            Log.v(TAG, "Decrypting " + encryptedLength + "b data to " + decryptedLength + "b of decrypted data");
            decryptCipher.doFinal(
                    in.nioBuffer(in.readerIndex(), encryptedLength),
                    out.nioBuffer(out.writerIndex(), decryptedLength));
            out.writerIndex(out.writerIndex() + decryptedLength);
            in.readerIndex(in.readerIndex() + encryptedLength);

            decoded.add(out);
        } catch (GeneralSecurityException e) {
            ctx.close();
            throw e;
        }
    }
}