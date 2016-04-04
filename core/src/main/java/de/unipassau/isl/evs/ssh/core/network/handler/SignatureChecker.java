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
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import static de.unipassau.isl.evs.ssh.core.network.handler.SignatureGenerator.MESSAGE_SIGN_ALG;

/**
 * The SignatureChecker class is a channel handler that is part of a ChannelPipeline and checks signatures of received messages.
 *
 * @author Niko Fink
 */
public class SignatureChecker extends ChannelHandlerAdapter {
    private static final String TAG = SignatureChecker.class.getSimpleName();

    private final Signature verifySignature;

    public SignatureChecker(PublicKey remotePublicKey) throws GeneralSecurityException {
        verifySignature = Signature.getInstance(MESSAGE_SIGN_ALG);
        verifySignature.initVerify(remotePublicKey);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof ByteBuf) {
                final ByteBuf in = (ByteBuf) msg;
                final int dataLength = in.readInt();
                final ByteBuf data = in.readSlice(dataLength);
                final int signatureLength = in.readInt();
                final byte[] signature = new byte[signatureLength];
                in.readBytes(signature);

                verifySignature.update(data.nioBuffer());
                final boolean valid = verifySignature.verify(signature);
                //Log.v(TAG, "Read " + dataLength + "b of data with " + signatureLength + "b " +
                //        (valid ? "valid" : "invalid") + " signature" +
                //        (Log.isLoggable(TAG, Log.VERBOSE) ? ": " + Arrays.toString(signature) : ""));
                if (valid) {
                    data.retain();
                    ctx.fireChannelRead(data);
                } else {
                    throw new SignatureException("Message has a broken signature, closing connection");
                }
            } else {
                throw new SignatureException("Can't check signature of message of type " + (msg != null ? msg.getClass() : "null")
                        + ", closing connection");
            }
        } catch (SignatureException | RuntimeException e) {
            ctx.close();
            throw e;
        }
    }
}