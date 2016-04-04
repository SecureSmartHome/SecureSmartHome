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