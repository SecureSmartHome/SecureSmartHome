package de.unipassau.isl.evs.ssh.slave.handler;

import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * This handler receives an event when the door bell is rang, generates a message containing
 * information for this event and sends this information to the master.
 */
public class SlaveDoorBellHandler implements Handler {
    @Override
    public void handle(Message message) {
        //TODO implement
        throw new UnsupportedOperationException();
    }
}