package de.unipassau.isl.evs.ssh.core.network.handler;

import android.util.Log;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

import javax.crypto.Cipher;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * The Encrypter class is a channel handler that Is part of a ChannelPipeline and provides encryption for system messages.
 *
 * @author Niko Fink
 */
public class Encrypter extends MessageToByteEncoder<ByteBuf> {
    private static final String TAG = Encrypter.class.getSimpleName();
    static final String MESSAGE_CRYPT_ALG = "ECIES";

    private final Cipher encryptCipher;

    public Encrypter(PublicKey remotePublicKey) throws GeneralSecurityException {
        encryptCipher = Cipher.getInstance(MESSAGE_CRYPT_ALG);
        encryptCipher.init(Cipher.ENCRYPT_MODE, remotePublicKey);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        final int decryptedLength = in.readableBytes();
        final int encryptedLength = encryptCipher.getOutputSize(decryptedLength);
        //Log.v(TAG, "Encrypting " + decryptedLength + "b data to " + encryptedLength + "b of encrypted data");
        out.writeInt(encryptedLength);

        out.ensureWritable(encryptedLength);
        final ByteBuffer inNio = in.nioBuffer(in.readerIndex(), decryptedLength);
        final ByteBuffer outNio = out.nioBuffer(out.writerIndex(), encryptedLength);
        encryptCipher.doFinal(inNio, outNio);
        if (inNio.hasRemaining()) {
            Log.wtf(TAG, "Crypto library did not read all bytes for encryption (" + inNio.remaining() + " remaining)");
        }
        if (outNio.hasRemaining()) {
            Log.wtf(TAG, "Crypto library did not write all bytes for encryption (" + outNio.remaining() + " remaining)");
        }
        out.writerIndex(out.writerIndex() + encryptedLength);
        in.readerIndex(in.readerIndex() + decryptedLength);
    }
}