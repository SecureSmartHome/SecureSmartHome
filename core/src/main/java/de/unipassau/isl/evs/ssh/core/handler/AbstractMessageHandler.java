package de.unipassau.isl.evs.ssh.core.handler;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

public abstract class AbstractMessageHandler implements MessageHandler {
    private IncomingDispatcher dispatcher;

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void handlerRemoved(String routingKey) {}

    protected IncomingDispatcher getDispatcher() {
        return dispatcher;
    }

    protected Container getContainer() {
        return getDispatcher() != null ? getDispatcher().getContainer() : null;
    }

    protected <T extends Component> T getComponent(Key<T> key) {
        Container container = getContainer();
        if (container != null) {
            return container.get(key);
        } else {
            return null;
        }
    }

    protected <T extends Component> T requireComponent(Key<T> key) {
        Container container = getContainer();
        if (container != null) {
            return container.require(key);
        } else {
            throw new IllegalStateException("Handler not registered to a IncomingDispatcher");
        }
    }

    protected Message.AddressedMessage sendMessage(DeviceID toID, String routingKey, Message msg) {
        return requireComponent(OutgoingRouter.KEY).sendMessage(toID, routingKey, msg);
    }

    protected Message.AddressedMessage sendMessageLocal(String routingKey, Message msg) {
        return requireComponent(OutgoingRouter.KEY).sendMessageLocal(routingKey, msg);
    }

    /**
     * Respond with an error message to a given AddressedMessage.
     *
     * @param original Original Message.
     */
    protected Message.AddressedMessage sendErrorMessage(Message.AddressedMessage original) {
        //FIXME logging, reasons??
        Message reply = new Message(new MessageErrorPayload(original.getPayload()));
        reply.putHeader(Message.HEADER_REFERENCES_ID, original.getSequenceNr());
        return sendMessage(
                original.getFromID(),
                original.getHeader(Message.HEADER_REPLY_TO_KEY),
                reply
        );
    }

    /**
     * Method that returns a String for the given handler
     *
     * @return String for the given handler
     */
    @Override
    public String toString() {
        String name = getClass().getSimpleName();
        if (name.isEmpty()) {
            name = getClass().getName();
            int index = name.lastIndexOf(".");
            if (index >= 0 && index + 1 < name.length()) {
                name = name.substring(index + 1);
            }
        }
        return name;
    }
}
