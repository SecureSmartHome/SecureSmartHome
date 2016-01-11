package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Log;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.RegisterSlavePayload;
import de.unipassau.isl.evs.ssh.master.database.AlreadyInUseException;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_SLAVE_REGISTER;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.ADD_ODROID;

/**
 * Handles messages indicating that information of a device needs to be updated and writes these changes to the routing table.
 * <p/>
 * An example when this handler needs to take action is when a new sensor is added or switched to a new GPIO Pin.
 *
 * @author Leon Sell
 */
public class MasterSlaveManagementHandler extends ModuleBroadcastHandler implements Component {
    public static final Key<MasterSlaveManagementHandler> KEY = new Key<>(MasterSlaveManagementHandler.class);
    private static final String TAG = MasterSlaveManagementHandler.class.getSimpleName();

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_SLAVE_REGISTER};
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_SLAVE_REGISTER.matches(message)) {
            handleSlaveRegister(message);
            //TODO Leon handle MASTER_SLAVE_DELETE
        } else {
            invalidMessage(message);
        }
    }

    private void handleSlaveRegister(Message.AddressedMessage message) {
        if (hasPermission(message.getFromID(), ADD_ODROID)) {
            final RegisterSlavePayload registerSlavePayload = MASTER_SLAVE_REGISTER.getPayload(message);
            try {
                registerSlave(new Slave(
                        registerSlavePayload.getName(),
                        registerSlavePayload.getSlaveID(),
                        registerSlavePayload.getPassiveRegistrationToken()
                ));
            } catch (AlreadyInUseException e) {
                Log.i(TAG, "Failed adding a new Slave because the given name (" + registerSlavePayload.getName()
                        + ") is already in use.");
                sendReply(message, new Message(new ErrorPayload("Failed adding a new Slave because the given name ("
                        + registerSlavePayload.getName() + ") is already in use.")));
            }
        } else {
            sendNoPermissionReply(message, ADD_ODROID);
        }
    }

    public void registerSlave(Slave slave) throws AlreadyInUseException {
        requireComponent(SlaveController.KEY).addSlave(slave);
        updateAllClients();
    }
}