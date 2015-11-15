package de.unipassau.isl.evs.ssh.slave.handler;

import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * Handles messages requesting pictures from the camera (via API calls) and generates messages,
 * containing the pictures, and sends these to the master.
 */
public class SlaveCameraHandler implements Handler {
    @Override
    public void handle(Message message) {
        //TODO implement
        throw new UnsupportedOperationException();
    }
}