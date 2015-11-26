package de.unipassau.isl.evs.ssh.slave.handler;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;

/**
 * This handler receives an event when the door bell is rang, generates a message containing
 * information for this event and sends this information to the master.
 */
public class SlaveDoorBellHandler implements MessageHandler {

    private String modulName;
    private IncomingDispatcher dispatcher;

    @Override
    public void handle(Message.AddressedMessage message) {
        //TODO register listener
        sendDoorBellInfo();
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void handlerRemoved(String routingKey) {

    }

    /**
     * Sends info about doorbell being used
     */
    private void sendDoorBellInfo() {
        DoorBellPayload payload = new DoorBellPayload(modulName);

        NamingManager namingManager = dispatcher.getContainer().require(NamingManager.KEY);

        Message message;
        message = new Message(payload);
        message.putHeader(Message.HEADER_TIMESTAMP, System.currentTimeMillis());

        OutgoingRouter router = dispatcher.getContainer().require(OutgoingRouter.KEY);
        router.sendMessage(namingManager.getMasterID(), CoreConstants.RoutingKeys.MASTER_DOOR_BELL_RING, message);
    }
}