package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Log;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.RegisterSlavePayload;
import de.unipassau.isl.evs.ssh.master.database.AlreadyInUseException;
import de.unipassau.isl.evs.ssh.master.database.DatabaseContract;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

/**
 * Handles messages indicating that information of a device needs to be updated and writes these changes to the routing table.
 * <p/>
 * An example when this handler needs to take action is when a new sensor is added or switched to a new GPIO Pin.
 * @author leon
 */
public class MasterRoutingTableHandler extends AbstractMasterHandler {
    private static final String TAG = MasterRoutingTableHandler.class.getSimpleName();

    @Override
    public void handle(Message.AddressedMessage message) {
        saveMessage(message);
        if (message.getPayload() instanceof RegisterSlavePayload) {
            if (hasPermission(message.getFromID(), new Permission(DatabaseContract.Permission.Values.ADD_ORDROID))) {
                switch (message.getRoutingKey()) {
                    case CoreConstants.RoutingKeys.MASTER_SLAVE_REGISTER:
                        handleRegisterRequest(message);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported routing key: " + message.getRoutingKey()
                                + " for RegisterSlavePayload.");
                }
            }
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            handleErrorMessage(message);
        } else {
            sendErrorMessage(message);
        }
    }

    private void handleRegisterRequest(Message.AddressedMessage message) {
        RegisterSlavePayload registerSlavePayload = ((RegisterSlavePayload) message.getPayload());
        try {
            requireComponent(SlaveController.KEY).addSlave(new Slave(registerSlavePayload.getName(),
                    registerSlavePayload.getSlaveID()));
        } catch (AlreadyInUseException e) {
            Log.i(TAG, "Failed adding a new Slave because the given name (" + registerSlavePayload.getName()
                    + ") is already in use.");
            sendErrorMessage(message);
        }
    }
}