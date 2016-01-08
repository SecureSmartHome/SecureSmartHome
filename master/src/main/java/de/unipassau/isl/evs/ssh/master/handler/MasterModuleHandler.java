package de.unipassau.isl.evs.ssh.master.handler;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModifyModulePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.sec.Permission;
import de.unipassau.isl.evs.ssh.master.database.DatabaseControllerException;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.network.Server;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.GLOBAL_MODULES_UPDATE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DEVICE_CONNECTED;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_ADD;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_REMOVE;

/**
 * The MasterModuleHandler sends updated lists of active Modules to ODROIDs and Clients
 *
 * @author Andreas Bucher
 * @author Wolfgang Popp
 */
public class MasterModuleHandler extends AbstractMasterHandler {

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_MODULE_ADD, MASTER_MODULE_REMOVE, MASTER_DEVICE_CONNECTED};
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_DEVICE_CONNECTED.matches(message)) {
            DeviceID deviceID = MASTER_DEVICE_CONNECTED.getPayload(message).deviceID;
            updateClient(deviceID);
        } else if (MASTER_MODULE_ADD.matches(message)) {
            addModule(MASTER_MODULE_ADD.getPayload(message), message);
        } else if (MASTER_MODULE_REMOVE.matches(message)) {
            removeModule(MASTER_MODULE_REMOVE.getPayload(message), message);
        } else {
            invalidMessage(message);
        }
    }

    private void removeModule(ModifyModulePayload payload, Message.AddressedMessage original) {
        DeviceID fromID = original.getFromID();

        if (!hasPermission(fromID, Permission.DELETE_SENSOR, null)) {
            sendNoPermissionReply(original, Permission.DELETE_SENSOR);
            return;
        }

        requireComponent(SlaveController.KEY).removeModule(payload.getModule().getName());
        sendOnSuccess(original);
    }

    private void addModule(ModifyModulePayload payload, Message.AddressedMessage original) {
        DeviceID fromID = original.getFromID();

        if (!hasPermission(fromID, Permission.ADD_SENSOR, null)) {
            sendNoPermissionReply(original, Permission.ADD_SENSOR);
            return;
        }

        Module module = payload.getModule();
        SlaveController slaveController = requireComponent(SlaveController.KEY);
        PermissionController permissionController = requireComponent(PermissionController.KEY);
        Permission[] permissions = Permission.getPermissions(module.getModuleType());

        try {
            slaveController.addModule(module);

            if (permissions != null) {
                for (Permission permission : permissions) {
                    permissionController.addPermission(permission, module.getName());
                }
            }

            sendOnSuccess(original);
        } catch (DatabaseControllerException e) {
            sendError(original, e);
        }
    }

    private void sendOnSuccess(Message.AddressedMessage original) {
        updateAllClients();
        sendReply(original, new Message());
    }

    private Message createUpdateMessage() {
        SlaveController slaveController = requireComponent(SlaveController.KEY);
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
        Message message = createUpdateMessage();
        sendMessage(id, GLOBAL_MODULES_UPDATE, message);
    }

    private void sendError(Message.AddressedMessage original, Exception e) {
        sendReply(original, new Message(new ErrorPayload(e)));
    }
}
