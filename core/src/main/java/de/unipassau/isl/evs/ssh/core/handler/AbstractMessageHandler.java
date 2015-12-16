package de.unipassau.isl.evs.ssh.core.handler;

import java.util.HashSet;
import java.util.Set;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

public abstract class AbstractMessageHandler implements MessageHandler {
    private Container container;
    private IncomingDispatcher dispatcher;
    private final Set<RoutingKey> registeredKeys = new HashSet<>(getRoutingKeys().length);

    public abstract RoutingKey[] getRoutingKeys();

    public void init(Container container) {
        this.container = container;
        getDispatcher().registerHandler(this, getRoutingKeys());
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, RoutingKey routingKey) {
        registeredKeys.add(routingKey);
        this.dispatcher = dispatcher;
    }

    @Override
    public void handlerRemoved(RoutingKey routingKey) {
        registeredKeys.remove(routingKey);
        if (registeredKeys.isEmpty()) {
            dispatcher = null;
        }
    }

    public void destroy() {
        getDispatcher().unregisterHandler(this, getRoutingKeys());
        this.container = null;
    }

    protected IncomingDispatcher getDispatcher() {
        return dispatcher == null ? container.get(IncomingDispatcher.KEY) : dispatcher;
    }

    protected Container getContainer() {
        return dispatcher == null ? container : dispatcher.getContainer();
    }

    protected boolean isActive() {
        return container != null;
    }

    protected boolean isRegistered() {
        return dispatcher != null;
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

    protected Message.AddressedMessage sendMessage(DeviceID toID, RoutingKey routingKey, Message msg) {
        return requireComponent(OutgoingRouter.KEY).sendMessage(toID, routingKey, msg);
    }

    protected Message.AddressedMessage sendMessageLocal(RoutingKey routingKey, Message msg) {
        return requireComponent(OutgoingRouter.KEY).sendMessageLocal(routingKey, msg);
    }

    protected Message.AddressedMessage sendMessageToMaster(RoutingKey routingKey, Message msg) {
        return requireComponent(OutgoingRouter.KEY).sendMessageToMaster(routingKey, msg);
    }

    /**
     * Respond with an error message to a given AddressedMessage.
     *
     * @param original Original Message.
     */
    @Deprecated
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

    protected void invalidMessage(Message.AddressedMessage message) {
        //TODO implement (Niko 2015-12-16)
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
