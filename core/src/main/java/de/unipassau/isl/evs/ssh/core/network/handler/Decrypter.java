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
 *
 * @author Niko Fink
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
            //Log.v(TAG, "Decrypting " + encryptedLength + "b data to " + decryptedLength + "b of decrypted data");
            decryptCipher.doFinal(
                    in.nioBuffer(in.readerIndex(), encryptedLength),
                    out.nioBuffer(out.writerIndex(), decryptedLength));
            out.writerIndex(out.writerIndex() + decryptedLength);
            in.readerIndex(in.readerIndex() + encryptedLength);

            decoded.add(out);
        } catch (GeneralSecurityException | RuntimeException e) {
            ctx.close();
            throw e;
        }
    }
}