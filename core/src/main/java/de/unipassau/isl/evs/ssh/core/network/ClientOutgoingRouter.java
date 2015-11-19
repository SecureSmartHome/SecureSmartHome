package de.unipassau.isl.evs.ssh.core.network;

import android.util.Log;

import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.util.DeviceID;
import io.netty.channel.ChannelFuture;

/**
 * Receives messages from system components and decides how to route them to the targets.
 */
public class ClientOutgoingRouter extends AbstractComponent implements OutgoingRouter {
    private static final String TAG = ClientOutgoingRouter.class.getSimpleName();

    @Override
    public ChannelFuture sendMessageLocal(String routingKey, Message message) {
        return sendMessage(getLocalID(), routingKey, message);
    }

    /**
     * Forwards an add to the ChannelPipeline which is in charge of the connection to the target specified in the AddressedMessage.
     *
     * @param toID       ID of the receiving device.
     * @param routingKey Alias of the receiving Handler.
     * @param msg        AddressedMessage to forward.
     */
    @Override
    public ChannelFuture sendMessage(DeviceID toID, String routingKey, Message msg) {
        Message.AddressedMessage amsg = msg.setDestination(getLocalID(), toID, routingKey);
        if (amsg.getToID().equals(getLocalID())) {
            requireComponent(IncomingDispatcher.KEY).dispatch(amsg);
            return requireComponent(Client.KEY).getChannel().newSucceededFuture();
        } else if (amsg.getToID().equals(getMasterID())) {
            return requireComponent(Client.KEY).getChannel().writeAndFlush(amsg);
        } else {
            IllegalArgumentException e = new IllegalArgumentException("Client " + getLocalID() + " can't send message to other client " + toID);
            e.fillInStackTrace();
            Log.w(TAG, "sendMessage failed", e);
            return requireComponent(Client.KEY).getChannel().newFailedFuture(e);
        }
    }

    private DeviceID getLocalID() {
        //require("KeyStoreManager").getLocalID(); //TODO
        return null;
    }

    private DeviceID getMasterID() {
        //require("KeyStoreManager").getLocalID(); //TODO
        return null;
    }
}
