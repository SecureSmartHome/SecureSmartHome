package de.unipassau.isl.evs.ssh.core.messaging;

import android.util.Log;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
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
        final Message.AddressedMessage amsg = msg.setDestination(getOwnID(), toID, routingKey);
        final Future<Void> future = doSendMessage(amsg);
        amsg.setSendFuture(future);
        future.addListener(new GenericFutureListener<Future<Void>>() {
            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                Log.v(TAG, "SENT " + amsg);
                if (!future.isSuccess()) {
                    Log.w(TAG, "Could not send Message " + amsg + " because of " + Log.getStackTraceString(future.cause()));
                }
            }
        });
        return amsg;
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
        return sendMessage(original.getFromID(), RoutingKey.getReplyKey(original.getRoutingKey()), reply);
    }

    protected DeviceID getMasterID() {
        return requireComponent(NamingManager.KEY).getMasterID();
    }

    protected DeviceID getOwnID() {
        return requireComponent(NamingManager.KEY).getOwnID();
    }
}
