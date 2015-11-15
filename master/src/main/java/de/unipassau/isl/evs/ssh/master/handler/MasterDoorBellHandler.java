package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * Handles messages received when the doorbell is used, requests a picture from the camera
 * (by also sending a message to the MasterCameraHandler) and generates messages for each target
 * and passes them to the OutgoingRouter.
 */
public class MasterDoorBellHandler implements Handler {
    @Override
    public void handle(Message message) {
        //TODO implement
        throw new UnsupportedOperationException();
    }
}