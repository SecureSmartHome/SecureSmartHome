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
 * Convenience base class for MessageHandles that can handle exactly one type of Message.
 *
 * @author Niko Fink
 */
public abstract class SimpleMessageHandler<T> extends AbstractMessageHandler {
    protected final RoutingKey<T> routingKey;

    protected SimpleMessageHandler(RoutingKey<T> routingKey) {
        this.routingKey = routingKey;
    }

    protected abstract void handleRouted(Message.AddressedMessage message, T payload);

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{routingKey};
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (routingKey.matches(message)) {
            handleRouted(message, routingKey.getPayload(message));
        } else {
            invalidMessage(message);
        }
    }
}
