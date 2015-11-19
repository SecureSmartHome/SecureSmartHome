package de.unipassau.isl.evs.ssh.master.network;

import java.io.IOException;

import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.util.DeviceID;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

/**
 * Receives messages from system components and decides how to route them to the targets.
 */
public class ServerOutgoingRouter extends AbstractComponent implements OutgoingRouter {
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
            return requireComponent(Server.KEY).getChannel().newSucceededFuture();
        } else {
            Channel channel = requireComponent(Server.KEY).findChannel(amsg.getToID());
            if (channel == null || !channel.isOpen()) {
                Exception e = new IOException("Client " + amsg.getToID() + " is not connected");
                e.fillInStackTrace();
                return requireComponent(Server.KEY).getChannel().newFailedFuture(e);
            } else {
                return channel.writeAndFlush(amsg);
            }
        }
    }

    public DeviceID getLocalID() {
        //require("KeyStoreManager").getLocalID(); //TODO
        return null;
    }
}
