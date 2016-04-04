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

package de.unipassau.isl.evs.ssh.core.handler;

import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;

/**
 * Convenience base class for MessageHandles that can handle exactly two types of Messages.
 *
 * @author Niko Fink
 */
public abstract class DoubleMessageHandler<T, U> extends AbstractMessageHandler {
    protected final RoutingKey<T> routingKey1;
    protected final RoutingKey<U> routingKey2;

    protected DoubleMessageHandler(RoutingKey<T> routingKey1, RoutingKey<U> routingKey2) {
        this.routingKey1 = routingKey1;
        this.routingKey2 = routingKey2;
    }

    protected abstract void handleRouted1(Message.AddressedMessage message, T payload);

    protected abstract void handleRouted2(Message.AddressedMessage message, U payload);

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{routingKey1, routingKey2};
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (routingKey1.matches(message)) {
            handleRouted1(message, routingKey1.getPayload(message));
        } else if (routingKey2.matches(message)) {
            handleRouted2(message, routingKey2.getPayload(message));
        } else {
            invalidMessage(message);
        }
    }
}
