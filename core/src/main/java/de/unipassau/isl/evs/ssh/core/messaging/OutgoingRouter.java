package de.unipassau.isl.evs.ssh.core.messaging;

import android.util.Log;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

/**
 * Receives messages from system components and decides how to route them to the targets.
 *
 * @author Niko Fink
 */
public abstract class OutgoingRouter extends AbstractComponent {
    public static final Key<OutgoingRouter> KEY = new Key<>(OutgoingRouter.class);

    /**
     * Forwards the message to the correct internal Server or Client pipeline, depending on the
     * {@code de.unipassau.isl.evs.ssh.core.messaging.Message.AddressedMessage#toID toID} of the message.
     */
    protected abstract ChannelFuture doSendMessage(Message.AddressedMessage message);

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
        final ChannelFuture future = doSendMessage(amsg);
        amsg.setSendFuture(future);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    Log.w(OutgoingRouter.this.getClass().getSimpleName(),
                            "Could not send Message " + amsg, future.cause());
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

    protected DeviceID getMasterID() {
        return requireComponent(NamingManager.KEY).getMasterID();
    }

    protected DeviceID getOwnID() {
        return requireComponent(NamingManager.KEY).getOwnID();
    }
}
