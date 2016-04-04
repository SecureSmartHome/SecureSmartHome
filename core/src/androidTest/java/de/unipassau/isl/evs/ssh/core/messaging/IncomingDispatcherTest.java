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

package de.unipassau.isl.evs.ssh.core.messaging;

import junit.framework.TestCase;

import java.security.SignatureException;
import java.util.Arrays;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.schedule.DefaultExecutionServiceComponent;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
import io.netty.channel.embedded.EmbeddedChannel;

public class IncomingDispatcherTest extends TestCase {
    private static final RoutingKey<Void> ROUTING_KEY_1 = new RoutingKey<>("/test1", Void.class);
    private static final RoutingKey<Void> ROUTING_KEY_2 = new RoutingKey<>("/test2", Void.class);
    private static final DeviceID ID1;
    private static final DeviceID ID2;
    private static final DeviceID ID3;

    static {
        byte[] id = new byte[DeviceID.ID_LENGTH];
        Arrays.fill(id, (byte) 1);
        ID1 = new DeviceID(id);
        Arrays.fill(id, (byte) 2);
        ID2 = new DeviceID(id);
        Arrays.fill(id, (byte) 3);
        ID3 = new DeviceID(id);
    }

    private final Container container = new SimpleContainer();
    private final IncomingDispatcher dispatcher = new IncomingDispatcher();
    private final EmbeddedChannel channel = new EmbeddedChannel(dispatcher);

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        container.register(ExecutionServiceComponent.KEY, new DefaultExecutionServiceComponent("test"));
        container.register(IncomingDispatcher.KEY, dispatcher);
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

    public void testUnauthenticated() {
        try {
            channel.writeInbound(new Message().setDestination(ID1, ID2, "/test"));
            channel.runPendingTasks();
            channel.checkException();
            fail("Missing exception");
        } catch (IllegalStateException e) {
            assertEquals("Unauthenticated peer", e.getMessage());
        }
    }

    public void testSpoofedID() {
        try {
            channel.attr(CoreConstants.NettyConstants.ATTR_PEER_ID).set(ID3);
            channel.writeInbound(new Message().setDestination(ID1, ID2, "/test"));
            channel.runPendingTasks();
            channel.checkException();
            if (false) throw new SignatureException();
            fail("Missing exception");
        } catch (SignatureException e) {
        }
    }

    public void testUnknownID() {
        channel.attr(CoreConstants.NettyConstants.ATTR_PEER_ID).set(ID1);
        final Message.AddressedMessage message = new Message().setDestination(ID1, ID2, "/test");
        channel.writeInbound(message);
        channel.runPendingTasks();
        assertEquals(channel.readInbound(), message);
    }

    public void testDispatch() {
        final TestHandler handler1 = new TestHandler();
        final TestHandler handler2 = new TestHandler();
        final TestHandler handler3 = new TestHandler();
        dispatcher.registerHandler(handler1, ROUTING_KEY_1);
        dispatcher.registerHandler(handler2, ROUTING_KEY_1);
        dispatcher.registerHandler(handler3, ROUTING_KEY_2);

        channel.attr(CoreConstants.NettyConstants.ATTR_PEER_ID).set(ID1);
        channel.writeInbound(new Message().setDestination(ID1, ID2, ROUTING_KEY_1.getKey()));
        channel.runScheduledPendingTasks();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        channel.checkException();

        assertTrue(handler1.toString(), handler1.ran);
        assertTrue(handler2.toString(), handler2.ran);
        assertFalse(handler3.toString(), handler3.ran);
    }

    public void testSimultaneousDispatch() {
        final TestHandler[] others = new TestHandler[4];
        others[0] = new TestHandler(200, others);
        others[1] = new TestHandler(200, others);
        others[2] = new TestHandler(200, others);
        others[3] = new TestHandler(200, others);
        dispatcher.registerHandler(others[0], ROUTING_KEY_1);
        dispatcher.registerHandler(others[1], ROUTING_KEY_1);
        dispatcher.registerHandler(others[2], ROUTING_KEY_1);
        dispatcher.registerHandler(others[3], ROUTING_KEY_1);

        channel.attr(CoreConstants.NettyConstants.ATTR_PEER_ID).set(ID1);
        channel.writeInbound(new Message().setDestination(ID1, ID2, ROUTING_KEY_1.getKey()));
        channel.runScheduledPendingTasks();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        channel.checkException();

        assertTrue(Arrays.toString(others), others[0].ran);
        assertTrue(Arrays.toString(others), others[1].ran);
        assertTrue(Arrays.toString(others), others[2].ran);
        assertTrue(Arrays.toString(others), others[3].ran);
    }

    private static class TestHandler implements MessageHandler {
        private boolean running;
        private boolean ran;
        private final int delay;
        private final TestHandler[] others;

        private TestHandler() {
            this(0, null);
        }

        private TestHandler(int delay, TestHandler... others) {
            this.delay = delay;
            this.others = others;
        }

        @Override
        public void handle(Message.AddressedMessage message) {
            try {
                System.out.println(Arrays.toString(others));
                running = true;
                checkConcurrency();
                if (delay > 0) {
                    Thread.sleep(delay);
                }
                checkConcurrency();
                ran = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                running = false;
                System.out.println(Arrays.toString(others));
            }
        }

        private void checkConcurrency() {
            if (others != null) {
                for (TestHandler other : others) {
                    if (other == this) continue;
                    assertFalse(other.running);
                }
            }
        }

        @Override
        public void handlerAdded(IncomingDispatcher dispatcher, RoutingKey routingKey) { }

        @Override
        public void handlerRemoved(RoutingKey routingKey) { }

        @Override
        public String toString() {
            return "TestHandler{" +
                    "running=" + running +
                    ", ran=" + ran +
                    ", delay=" + delay +
                    '}';
        }
    }
}