package de.unipassau.isl.evs.ssh.core.handler;

import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;

/**
 * Handlers perform actions based on the received messages and there field of responsibility.
 * In case the executed actions result in the need of notifying another system component,
 * a new message is generated and passed on to the OutgoingRouter.
 *
 * @author Niko Fink
 */
public interface MessageHandler {
    /**
     * Will perform actions based on the message given, e.g. permission/sanity checks.
     *
     * @param message Message to handle.
     */
    void handle(Message.AddressedMessage message);

    /**
     * TODO JavaDoc
     *
     * @param dispatcher
     * @param routingKey
     */
    void handlerAdded(IncomingDispatcher dispatcher, RoutingKey routingKey);

    /**
     * TODO JavaDoc
     *
     * @param routingKey
     */
    void handlerRemoved(RoutingKey routingKey);
}

