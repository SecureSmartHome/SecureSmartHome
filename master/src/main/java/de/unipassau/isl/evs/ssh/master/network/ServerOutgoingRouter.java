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

package de.unipassau.isl.evs.ssh.master.network;

import java.io.IOException;
import java.util.Objects;

import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;

/**
 * Receives messages from system components and decides how to route them to the targets.
 *
 * @author Niko Fink
 */
public class ServerOutgoingRouter extends OutgoingRouter {
    @Override
    protected Future<Void> doSendMessage(Message.AddressedMessage amsg) {
        final ExecutionServiceComponent executionService = requireComponent(ExecutionServiceComponent.KEY);
        if (Objects.equals(amsg.getToID(), getOwnID())) {
            //Send Local
            requireComponent(IncomingDispatcher.KEY).dispatch(amsg);
            return executionService.newSucceededFuture(null);
        } else {
            //Find client and send the message there
            Channel channel = requireComponent(Server.KEY).findChannel(amsg.getToID());
            if (channel == null || !channel.isOpen()) {
                Exception e = new IOException("Client " + amsg.getToID() + " is not connected");
                e.fillInStackTrace();
                return executionService.newFailedFuture(e);
                //in a future version, pending messages could be queued instead of failed directly
            } else {
                return channel.writeAndFlush(amsg);
            }
        }
    }
}
