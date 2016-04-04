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

import android.test.InstrumentationTestCase;

import java.security.SignatureException;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;

public class SignerTest extends InstrumentationTestCase {
    private Container container;
    private EmbeddedChannel channel;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        container = new SimpleContainer();
        KeyStoreController keyStoreController = new KeyStoreController();
        container.register(ContainerService.KEY_CONTEXT, new ContainerService.ContextComponent(getInstrumentation().getContext()));
        container.register(KeyStoreController.KEY, keyStoreController);

        final SignatureChecker checker = new SignatureChecker(keyStoreController.getOwnCertificate().getPublicKey());
        final SignatureGenerator signer = new SignatureGenerator(keyStoreController.getOwnPrivateKey());
        channel = new EmbeddedChannel(checker, signer);
    }

    @Override
    protected void tearDown() throws Exception {
        container.shutdown();

        channel.runPendingTasks();
        channel.checkException();
        assertTrue(channel.inboundMessages().isEmpty());
        assertTrue(channel.outboundMessages().isEmpty());

        super.tearDown();
    }

    public void testRoundtrip() {
        ByteBuf buf = channel.alloc().buffer();
        for (int c : new int[]{1, 1, 5, 10, 50, 100, 500, 1000, 5000, 10000}) {
            buf.capacity(c);
            while (buf.writableBytes() > 0) {
                buf.writeByte(c);
            }

            channel.writeOutbound(buf.duplicate().retain());
            channel.runPendingTasks();
            for (Object msg; (msg = channel.readOutbound()) != null; ) {
                channel.writeInbound(msg);
            }
            assertEquals(buf, channel.readInbound());
        }
        ReferenceCountUtil.release(buf);
    }

    public void testModify() {
        ByteBuf buf = channel.alloc().buffer();
        buf.capacity(1000);
        while (buf.writableBytes() > 0) {
            buf.writeByte(buf.writableBytes());
        }

        try {
            channel.writeOutbound(buf.duplicate().retain());
            for (ByteBuf msg; (msg = channel.readOutbound()) != null; ) {
                msg.setByte(500, ~msg.getByte(500));
                channel.writeInbound(msg);
            }

            channel.checkException();

            if (false) throw new SignatureException();
            fail("Missing exception");
        } catch (SignatureException e) {
        }
        if (channel.isOpen()) {
            fail("Channel not closed");
        }
    }
}