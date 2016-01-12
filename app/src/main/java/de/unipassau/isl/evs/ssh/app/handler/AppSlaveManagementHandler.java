package de.unipassau.isl.evs.ssh.app.handler;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DeleteDevicePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.RegisterSlavePayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

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
public class AppSlaveManagementHandler extends AbstractAppHandler implements Component {
    public static final Key<AppSlaveManagementHandler> KEY = new Key<>(AppSlaveManagementHandler.class);
    private static final String TAG = AppSlaveManagementHandler.class.getSimpleName();

    private List<SlaveManagementListener> listeners = new LinkedList<>();

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                MASTER_SLAVE_REGISTER_REPLY,
                MASTER_SLAVE_REGISTER_ERROR,
                MASTER_SLAVE_DELETE_REPLY,
                MASTER_SLAVE_DELETE_ERROR
        };
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (!tryHandleResponse(message)) {
            if (MASTER_SLAVE_REGISTER_REPLY.matches(message)) {
                fireSlaveRegistered(true);
            } else if (MASTER_SLAVE_REGISTER_ERROR.matches(message)) {
                fireSlaveRegistered(false);
            } else if (MASTER_SLAVE_DELETE_REPLY.matches(message)) {
                fireSlaveRemoved(true);
            } else if (MASTER_SLAVE_DELETE_ERROR.matches(message)) {
                fireSlaveRemoved(false);
            } else {
                invalidMessage(message);
            }
        }
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
        final Future<Void> future = newResponseFuture(sendMessageToMaster(MASTER_SLAVE_REGISTER, new Message(payload)));
        future.addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                fireSlaveRegistered(future.isSuccess());
            }
        });
    }

    public void deleteSlave(DeviceID slaveID) {
        DeleteDevicePayload payload = new DeleteDevicePayload(slaveID);
        final Future<Void> future = newResponseFuture(sendMessageToMaster(MASTER_SLAVE_DELETE, new Message(payload)));
        future.addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                fireSlaveRemoved(future.isSuccess());
            }
        });
    }

    public void addSlaveManagemntListener(SlaveManagementListener listener) {
        listeners.add(listener);
    }

    public void removeSlaveManagemntListener(SlaveManagementListener listener) {
        listeners.remove(listener);
    }

    private void fireSlaveRegistered(boolean wasSuccessful) {
        for (SlaveManagementListener listener : listeners) {
            listener.onSlaveRegistered(wasSuccessful);
        }
    }

    private void fireSlaveRemoved(boolean wasSuccessful) {
        for (SlaveManagementListener listener : listeners) {
            listener.onSlaveRemoved(wasSuccessful);
        }
    }

    public interface SlaveManagementListener {
        void onSlaveRegistered(boolean wasSuccessful);

        void onSlaveRemoved(boolean wasSuccessful);
    }
}
