package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * Handles messages indicating that information of a device needs to be updated and writes these changes to the routing table.
 * <p/>
 * An example when this handler needs to take action is when a new sensor is added or switched to a new GPIO Pin.
 */
public class MasterRoutingTableHandler implements MessageHandler {

    @Override
    public void handle(Message.AddressedMessage message) {

    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {

    }
}