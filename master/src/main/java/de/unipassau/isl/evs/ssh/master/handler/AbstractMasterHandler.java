package de.unipassau.isl.evs.ssh.master.handler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

public abstract class AbstractMasterHandler implements MessageHandler {
    protected IncomingDispatcher incomingDispatcher;
    protected PermissionController permissionController;
    protected Container container;
    protected OutgoingRouter outgoingRouter;
    protected SlaveController slaveController;
    private Map<Integer, Message.AddressedMessage> inbox = new HashMap<>();
    private Map<Integer, Integer> onBehalfOfMessage = new HashMap<>();
    protected Set<String> routingKeys = new HashSet<>();

    @Override
    public void handle(Message.AddressedMessage message) {

    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
        routingKeys.add(routingKey);
        incomingDispatcher = dispatcher;
        container = dispatcher.getContainer();
        outgoingRouter = container.require(OutgoingRouter.KEY);
        permissionController = container.require(PermissionController.KEY);
        slaveController = container.require(SlaveController.KEY);
    }

    @Override
    public void handlerRemoved(String routingKey) {
        routingKeys.remove(routingKey);
        if (routingKey.isEmpty()) {
            incomingDispatcher = null;
            container = null;
            outgoingRouter = null;
            permissionController = null;
            slaveController = null;
        }
    }

    protected void saveMessage(Message.AddressedMessage message) {
        //Todo clear sometime.
        inbox.put(message.getSequenceNr(), message);
    }

    protected Message.AddressedMessage getMessage(int sequenceNr) {
        return inbox.get(sequenceNr);
    }

    protected Message.AddressedMessage getMessageOnBehalfOfSequenceNr(int sequenceNr) {
        return getMessage(getSequenceNrOnBehalfOfSequenceNr(sequenceNr));
    }

    protected void putOnBehalfOf(int newMessageSequenceNr, int onBehalfOfMessageSequenceNr) {
        onBehalfOfMessage.put(newMessageSequenceNr, onBehalfOfMessageSequenceNr);

    }

    protected Integer getSequenceNrOnBehalfOfSequenceNr(int sequenceNr) {
        return onBehalfOfMessage.get(sequenceNr);
    }

    protected void sendErrorMessage(Message.AddressedMessage original) {
        Message reply = new Message(new MessageErrorPayload(original.getPayload()));
        reply.putHeader(Message.HEADER_REFERENCES_ID, original.getSequenceNr());
        outgoingRouter.sendMessage(original.getFromID(), original.getHeader(Message.HEADER_REPLY_TO_KEY), reply);
    }
}
