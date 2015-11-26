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
 */
public abstract class OutgoingRouter extends AbstractComponent {
    public static final Key<OutgoingRouter> KEY = new Key<>(OutgoingRouter.class);

    protected abstract ChannelFuture doSendMessage(Message.AddressedMessage message);

    /**
     * Forwards an add to the ChannelPipeline which is in charge of the connection to the target specified in the AddressedMessage.
     *
     * @param toID       ID of the receiving device.
     * @param routingKey Alias of the receiving Handler.
     * @param msg        AddressedMessage to forward.
     */
    public Message.AddressedMessage sendMessage(DeviceID toID, String routingKey, Message msg) {
        final Message.AddressedMessage amsg = msg.setDestination(getLocalID(), toID, routingKey);
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

    public Message.AddressedMessage sendMessageLocal(String routingKey, Message message) {
        return sendMessage(getLocalID(), routingKey, message);
    }

    public Message.AddressedMessage sendMessageToMaster(String routingKey, Message message) {
        return sendMessage(getMasterID(), routingKey, message);
    }

    protected DeviceID getMasterID() {
        return requireComponent(NamingManager.KEY).getMasterID();
    }

    protected DeviceID getLocalID() {
        return requireComponent(NamingManager.KEY).getLocalDeviceId();
    }
}
