package de.unipassau.isl.evs.ssh.core.handler;

import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * Handlers perform actions based on the received messages and there field of responsibility.
 * In case the executed actions result in the need of notifying another system component,
 * a new message is generated and passed on to the OutgoingRouter.
 */
public interface Handler {

    /**
     * Will perform actions based on the message given, e.g. permission/sanity checks.
     *
     * @param message Message to handle.
     */
    void handle(Message message);

}