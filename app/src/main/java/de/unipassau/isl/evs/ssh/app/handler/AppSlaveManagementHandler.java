package de.unipassau.isl.evs.ssh.app.handler;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DeleteDevicePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.RegisterSlavePayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_SLAVE_DELETE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_SLAVE_DELETE_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_SLAVE_DELETE_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_SLAVE_REGISTER;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_SLAVE_REGISTER_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_SLAVE_REGISTER_REPLY;

/**
 * The AppSlaveManagementHandler handles the messaging needed to add a new slave to the system.
 *
 * @author Wolfgang Popp
 */
public class AppSlaveManagementHandler extends AbstractMessageHandler implements Component {
    public static final Key<AppSlaveManagementHandler> KEY = new Key<>(AppSlaveManagementHandler.class);
    private static final String TAG = AppSlaveManagementHandler.class.getSimpleName();

    @Override
    public void handle(Message.AddressedMessage message) {
        //TODO fill
        if (MASTER_SLAVE_REGISTER_REPLY.matches(message)) {
        } else if (MASTER_SLAVE_REGISTER_ERROR.matches(message)) {
        } else if (MASTER_SLAVE_DELETE_REPLY.matches(message)) {
        } else if (MASTER_SLAVE_DELETE_ERROR.matches(message)) {
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                MASTER_SLAVE_REGISTER_REPLY,
                MASTER_SLAVE_REGISTER_ERROR,
                MASTER_SLAVE_DELETE_REPLY,
                MASTER_SLAVE_DELETE_ERROR
        };
    }

    /**
     * Sends a message to master to registers a new slave.
     *
     * @param slaveID                  the device ID of the new slave
     * @param slaveName                the name of the new slave
     * @param passiveRegistrationToken the passive Registration token
     */
    public void registerNewSlave(DeviceID slaveID, String slaveName, byte[] passiveRegistrationToken) {
        RegisterSlavePayload payload = new RegisterSlavePayload(slaveName, slaveID, passiveRegistrationToken);
        sendMessageToMaster(MASTER_SLAVE_REGISTER, new Message(payload));
    }

    public void deleteSlave(DeviceID slaveID) {
        DeleteDevicePayload payload = new DeleteDevicePayload(slaveID);
        sendMessageToMaster(MASTER_SLAVE_DELETE, new Message(payload));
    }

}
