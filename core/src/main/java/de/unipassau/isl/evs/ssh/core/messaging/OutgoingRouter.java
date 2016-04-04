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

import android.util.Log;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.AccessLogger;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Receives messages from system components and decides how to route them to the targets.
 *
 * @author Niko Fink
 */
public abstract class OutgoingRouter extends AbstractComponent {
    private static final String TAG = OutgoingRouter.class.getSimpleName();
    public static final Key<OutgoingRouter> KEY = new Key<>(OutgoingRouter.class);

    /**
     * Forwards the message to the correct internal Server or Client pipeline, depending on the
     * {@code de.unipassau.isl.evs.ssh.core.messaging.Message.AddressedMessage#toID toID} of the message.
     */
    protected abstract Future<Void> doSendMessage(Message.AddressedMessage message);

    private Message.AddressedMessage sendMessage(DeviceID toID, String routingKey, Message msg, boolean log) {
        final Message.AddressedMessage amsg = msg.setDestination(getOwnID(), toID, routingKey);
        final Future<Void> future = doSendMessage(amsg);
        amsg.setSendFuture(future);
        if (log) {
            future.addListener(new GenericFutureListener<Future<Void>>() {
                @Override
                public void operationComplete(Future<Void> future) throws Exception {
                    if (future.isSuccess()) {
                        Log.v(TAG, "SENT " + amsg);
                    } else {
                        Log.w(TAG, "Could not send Message " + amsg + " because of " + Log.getStackTraceString(future.cause()));
                    }
                }
            });
        }
        final AccessLogger logger = getComponent(AccessLogger.KEY);
        if (logger != null) {
            logger.logAccess(RoutingKey.forMessage(amsg));
        }
        return amsg;
    }

    /**
     * Adds the Address Information to the message by wrapping it in an immutable
     * {@link de.unipassau.isl.evs.ssh.core.messaging.Message.AddressedMessage} an sending to the corresponding
     * target.
     *
     * @param toID       ID of the receiving device.
     * @param routingKey Alias of the receiving Handler.
     * @param msg        AddressedMessage to forward.
     */
    public Message.AddressedMessage sendMessage(DeviceID toID, String routingKey, Message msg) {
        return sendMessage(toID, routingKey, msg, true);
    }

    /**
     * Forward the Message to the local {@link IncomingDispatcher}.
     *
     * @see IncomingDispatcher#dispatch(Message.AddressedMessage)
     */
    public Message.AddressedMessage sendMessageLocal(String routingKey, Message message) {
        return sendMessage(getOwnID(), routingKey, message);
    }

    /**
     * Send the Message to the Master.
     *
     * @see NamingManager#getMasterID()
     */
    public Message.AddressedMessage sendMessageToMaster(String routingKey, Message message) {
        return sendMessage(getMasterID(), routingKey, message);
    }

    /**
     * @throws IllegalArgumentException if the payload defined in the RoutingKey doesn't match the payload of the message.
     * @see #sendMessage(DeviceID, String, Message)
     */
    public Message.AddressedMessage sendMessage(DeviceID toID, RoutingKey routingKey, Message msg) {
        if (!routingKey.payloadMatches(msg)) {
            throw new IllegalArgumentException("Message payload does not match routing key " + routingKey + ":\n" + msg);
        }
        return sendMessage(toID, routingKey.getKey(), msg);
    }

    /**
     * @throws IllegalArgumentException if the payload defined in the RoutingKey doesn't match the payload of the message.
     * @see #sendMessageLocal(String, Message)
     */
    public Message.AddressedMessage sendMessageLocal(RoutingKey routingKey, Message msg) {
        if (!routingKey.payloadMatches(msg)) {
            throw new IllegalArgumentException("Message payload does not match routing key " + routingKey + ":\n" + msg);
        }
        return sendMessageLocal(routingKey.getKey(), msg);
    }

    /**
     * @throws IllegalArgumentException if the payload defined in the RoutingKey doesn't match the payload of the message.
     * @see #sendMessageToMaster(String, Message)
     */
    public Message.AddressedMessage sendMessageToMaster(RoutingKey routingKey, Message msg) {
        if (!routingKey.payloadMatches(msg)) {
            throw new IllegalArgumentException("Message payload does not match routing key " + routingKey + ":\n" + msg);
        }
        return sendMessageToMaster(routingKey.getKey(), msg);
    }

    /**
     * Sends a reply message to the device the original message came from.
     * Also sets the {@link Message#HEADER_REFERENCES_ID} of the sent message to the sequence number of the original message.
     *
     * @see #sendMessage(DeviceID, String, Message)
     */
    public Message.AddressedMessage sendReply(Message.AddressedMessage original, Message reply) {
        reply.putHeader(Message.HEADER_REFERENCES_ID, original.getSequenceNr());
        final Message.AddressedMessage amsg = sendMessage(
                original.getFromID(),
                RoutingKey.getReplyKey(original.getRoutingKey()),
                reply,
                !CoreConstants.TRACK_STATISTICS // don't use the default logger if TRACK_STATISTICS is set
        );
        if (CoreConstants.TRACK_STATISTICS) {
            final long originalTimestamp = original.getHeader(Message.HEADER_TIMESTAMP);
            amsg.getSendFuture().addListener(new GenericFutureListener<Future<Void>>() {
                @Override
                public void operationComplete(Future<Void> future) throws Exception {
                    final long replyTimestamp = amsg.getHeader(Message.HEADER_TIMESTAMP);
                    final long replyTime = replyTimestamp - originalTimestamp;
                    final long sendTime = System.currentTimeMillis() - replyTimestamp;
                    final long overallTime = replyTime + sendTime;
                    if (future.isSuccess()) {
                        Log.v(TAG, "SENT " + amsg + " after " + replyTime + "+" + sendTime + "=" + overallTime + "ms");
                    } else {
                        Log.w(TAG, "Could not send Message " + amsg + " because of " + Log.getStackTraceString(future.cause()));
                    }
                }
            });
        }
        return amsg;
    }

    protected DeviceID getMasterID() {
        return requireComponent(NamingManager.KEY).getMasterID();
    }

    protected DeviceID getOwnID() {
        return requireComponent(NamingManager.KEY).getOwnID();
    }
}
