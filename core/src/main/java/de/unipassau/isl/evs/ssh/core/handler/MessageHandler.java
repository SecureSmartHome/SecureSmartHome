package de.unipassau.isl.evs.ssh.core.handler;

import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;

/**
 * MessageHandlers perform actions based on the received messages such as processing and logging data and notifying other system components.
 *
 * @author Niko Fink
 */
public interface MessageHandler {
    /**
     * Will perform actions based on the message given, e.g. permission/sanity checks.
     *
     * @param message message to handle.
     */
    void handle(Message.AddressedMessage message);

    /**
     * Called by the {@link IncomingDispatcher} this Handler was added to with the RoutingKey the Handler was added for.
     */
    void handlerAdded(IncomingDispatcher dispatcher, RoutingKey routingKey);

    /**
     * Called by the {@link IncomingDispatcher} this Handler was added to once a mapping for a certain RoutingKey has been removed.
     */
    void handlerRemoved(RoutingKey routingKey);
}

