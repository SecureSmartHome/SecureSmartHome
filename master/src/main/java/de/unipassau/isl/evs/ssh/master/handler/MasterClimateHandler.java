package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * Handles climate messages and generates messages for each target and passes them to the OutgoingRouter.
 */
public class MasterClimateHandler implements Handler {
    @Override
    public void handle(Message message) {
        //TODO implement
        throw new UnsupportedOperationException();
    }
}