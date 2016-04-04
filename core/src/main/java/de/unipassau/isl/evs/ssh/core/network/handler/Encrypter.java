/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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