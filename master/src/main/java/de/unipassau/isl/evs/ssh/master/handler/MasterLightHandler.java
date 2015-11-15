package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * Handles light messages, logs them for the holiday simulation and generates messages
 * for each target and passes them to the OutgoingRouter.
 */
public class MasterLightHandler implements Handler {
    @Override
    public void handle(Message message) {
        //TODO implement
        throw new UnsupportedOperationException();
    }
}