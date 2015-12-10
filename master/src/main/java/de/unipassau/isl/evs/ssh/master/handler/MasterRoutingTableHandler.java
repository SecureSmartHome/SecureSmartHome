package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Log;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.RegisterSlavePayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.AlreadyInUseException;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.network.Server;

/**
 * Handles messages indicating that information of a device needs to be updated and writes these changes to the routing table.
 * <p/>
 * An example when this handler needs to take action is when a new sensor is added or switched to a new GPIO Pin.
 *
 * @author leon
 */
public class MasterRoutingTableHandler extends AbstractMasterHandler {
    private static final String TAG = MasterRoutingTableHandler.class.getSimpleName();

    @Override
    public void handle(Message.AddressedMessage message) {
        saveMessage(message);
        if (message.getPayload() instanceof RegisterSlavePayload) {
            if (hasPermission(message.getFromID(), new Permission(CoreConstants.Permission.BinaryPermission.ADD_ODROID.toString()))) {
                switch (message.getRoutingKey()) {
                    case CoreConstants.RoutingKeys.MASTER_SLAVE_REGISTER:
                        if (handleRegisterRequest(message)) {
                            updateAllClients();
                        }
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

    private boolean handleRegisterRequest(Message.AddressedMessage message) {
        RegisterSlavePayload registerSlavePayload = ((RegisterSlavePayload) message.getPayload());
        try {
            requireComponent(SlaveController.KEY).addSlave(new Slave(registerSlavePayload.getName(),
                    registerSlavePayload.getSlaveID()));
            return true;
        } catch (AlreadyInUseException e) {
            Log.i(TAG, "Failed adding a new Slave because the given name (" + registerSlavePayload.getName()
                    + ") is already in use.");
            sendErrorMessage(message);
        }
        return false;
    }

    private Message createUpdateMessage() {
        SlaveController slaveController = getComponent(SlaveController.KEY);
        List<Slave> slaves = slaveController.getSlaves();
        ListMultimap<Slave, Module> modulesAtSlave = ArrayListMultimap.create();

        for (Slave slave : slaves) {
            modulesAtSlave.putAll(slave, slaveController.getModulesOfSlave(slave.getSlaveID()));
        }

        return new Message(new ModulesPayload(modulesAtSlave, slaves));
    }

    private void updateAllClients() {
        Iterable<DeviceID> connectedClients = requireComponent(Server.KEY).getActiveDevices();
        for (DeviceID connectedClient : connectedClients) {
            updateClient(connectedClient);
        }
    }

    public void updateClient(DeviceID id) {
        OutgoingRouter router = getComponent(OutgoingRouter.KEY);
        Message message = createUpdateMessage();
        router.sendMessage(id, CoreConstants.RoutingKeys.MODULES_UPDATE, message);
    }
}