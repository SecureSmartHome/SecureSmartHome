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
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.AddNewModulePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.RenameModulePayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.DatabaseControllerException;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.network.Server;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.GLOBAL_MODULES_UPDATE;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_MODULE_ADD;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_MODULE_GET;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_MODULE_RENAME;

/**
 * The MasterModuleHandler sends updated lists of active Modules to ODROIDs and Clients
 *
 * @author Andreas Bucher
 * @author Wolfgang Popp
 */
public class MasterModuleHandler extends AbstractMasterHandler {
    private static final String TAG = MasterModuleHandler.class.getSimpleName();

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
        router.sendMessage(id, GLOBAL_MODULES_UPDATE, message);
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_MODULE_ADD.matches(message)) {
            AddNewModulePayload payload = MASTER_MODULE_ADD.getPayload(message);
            if (handleAddModule(payload.getModule(), message)) {
                updateAllClients();

                Message reply = new Message(new AddNewModulePayload(null));
                OutgoingRouter router = getComponent(OutgoingRouter.KEY);
                router.sendMessage(message.getFromID(), message.getHeader(Message.HEADER_REPLY_TO_KEY), reply);
            } else {
                //TODO handle (Niko, 2015-12-17)
            }
        } else if (MASTER_MODULE_GET.matches(message)) {
            sendMessage(message.getFromID(), message.getHeader(Message.HEADER_REPLY_TO_KEY), createUpdateMessage());
        /* @author Leon Sell */
        } else if (MASTER_MODULE_RENAME.matches(message)) {
            if (hasPermission(message.getFromID(), new Permission(CoreConstants.Permission.BinaryPermission.RENAME_MODULE.toString()))) {
                if (handleRenameModule(message)) {
                    updateAllClients();
                } else {
                    sendErrorMessage(message);
                }
            } else {
                //TODO handle (Niko, 2015-12-17)
            }
        } else {
            invalidMessage(message);
        }
        // TODO handle remove sensor
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_MODULE_ADD, MASTER_MODULE_GET, MASTER_MODULE_RENAME};
    }

    /* @author Leon Sell */
    private boolean handleRenameModule(Message.AddressedMessage message) {
        RenameModulePayload renameModulePayload = MASTER_MODULE_RENAME.getPayload(message);
        try {
            requireComponent(SlaveController.KEY).changeModuleName(renameModulePayload.getOldName(),
                    renameModulePayload.getNewName());
            return true;
        } catch (DatabaseControllerException e) {
            Log.e(TAG, "Error while adding new module: " + e.getCause().getMessage());
            sendErrorMessage(message);
            return false;
        }
    }

    private boolean handleAddModule(Module module, Message.AddressedMessage message) {
        SlaveController slaveController = requireComponent(SlaveController.KEY);
        PermissionController permissionController = requireComponent(PermissionController.KEY);
        boolean success = false;
        String[] permissions = CoreConstants.Permission.TernaryPermission.getPermissions(module.getModuleType());

        try {
            slaveController.addModule(module);

            if (permissions != null) {
                for (String permission : permissions) {
                    permissionController.addPermission(new Permission(permission, module.getName()));
                }
            }
            success = true;
        } catch (DatabaseControllerException e) {
            Log.e(TAG, "Error while adding new module: " + e.getCause().getMessage());
            sendErrorMessage(message);
        }
        return success;
    }

    private void handleRemoveModule(String moduleName, Message.AddressedMessage message) {
        SlaveController controller = getComponent(SlaveController.KEY);
        controller.removeModule(moduleName);
        // TODO send message
    }
}
