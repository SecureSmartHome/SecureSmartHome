package de.unipassau.isl.evs.ssh.core.messaging;

import de.unipassau.isl.evs.ssh.core.naming.OdroidID;
import io.netty.channel.ChannelFuture;

public class OutgoingRouter {
    // Mehrere Empfänger müssten bei diesen Ansatz vom Absender aufgelöst und dann einzeln explizit angesprochen werden.
    // Dafür ist der OutgoingRouter angenehm einfach.
    // Falls wir eine "findDevicesWithPermission" Methode anbieten wäre auch das auflösen einfach.

    public ChannelFuture sendMessageLocal(String routingKey, Message message) {
        return sendMessage(getLocalID(), routingKey, message);
    }

    public ChannelFuture sendMessage(OdroidID toID, String routingKey, Message message) {
        Message.AdressedMessage adressedMessage = message.setDestination(getLocalID(), toID, routingKey);
        if (adressedMessage.getToID().equals(getLocalID())) {
            //require("IncomingDispatcher").queue(addressedMessage);
        } else {
            //require("Server").findConnectionForID(toID).write(addressesMessage);
        }
        return null;
    }

    public OdroidID getLocalID() {
        //require("KeyStoreManager").getLocalID();
        return null;
    }
}
