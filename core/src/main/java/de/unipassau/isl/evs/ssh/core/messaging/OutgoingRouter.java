package de.unipassau.isl.evs.ssh.core.messaging;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.util.DeviceID;
import io.netty.channel.ChannelFuture;

/**
 * Receives messages from system components and decides how to route them to the targets.
 */
public class OutgoingRouter extends AbstractComponent {
    public static final Key<OutgoingRouter> KEY = new Key<>(OutgoingRouter.class);

    // Mehrere Empfänger müssten bei diesen Ansatz vom Absender aufgelöst und dann einzeln explizit angesprochen werden.
    // Dafür ist der OutgoingRouter angenehm einfach.
    // Falls wir eine "findDevicesWithPermission" Methode anbieten wäre auch das auflösen einfach.

    public ChannelFuture sendMessageLocal(String routingKey, Message message) {
        return sendMessage(getLocalID(), routingKey, message);
    }

    /**
     * Forwards an add to the ChannelPipeline which is in charge of the connection to the target specified in the AddressedMessage.
     *
     * @param toID       ID of the receiving device.
     * @param routingKey Alias of the receiving Handler.
     * @param message    AddressedMessage to forward.
     */
    public ChannelFuture sendMessage(DeviceID toID, String routingKey, Message message) {
        Message.AddressedMessage addressedMessage = message.setDestination(getLocalID(), toID, routingKey);
        if (addressedMessage.getToID().equals(getLocalID())) {
            //require("IncomingDispatcher").queue(addressedMessage);
        } else {
            //require("Server").findConnectionForID(toID).write(addressesMessage);
        }
        return null;
    }

    public DeviceID getLocalID() {
        //require("KeyStoreManager").getLocalID();
        return null;
    }
}
