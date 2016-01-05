package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;

/**
 * Handles messages indicating that information about permissions of devices are requested or
 * that permissions need to be updated and also writes changes to the database using the DatabaseConnector.
 * <p/>
 * TODO implement? (Niko, 2015-01-05)
 */
public class MasterPermissionHandler extends AbstractMasterHandler {
    @Override
    public void handle(Message.AddressedMessage message) {
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[0];
    }
}