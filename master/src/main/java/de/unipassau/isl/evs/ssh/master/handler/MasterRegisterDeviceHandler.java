package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * Handles messages indicating that a device wants to register itself at the system and also generates
 * messages for each target that needs to know of this event and passes them to the OutgoingRouter.
 *
 */
public class MasterRegisterDeviceHandler extends AbstractMasterHandler{

    @Override
    public void handle(Message.AddressedMessage message) {

        //TODO: if new user device gets added, send local message to UserConfigurationHandler to send update to users
    }
}