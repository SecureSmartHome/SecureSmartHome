package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Log;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

import java.util.List;
import java.util.Map;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DeviceConnectedPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.UserDeviceEditPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.UserDeviceInformationPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.DatabaseControllerException;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.database.UnknownReferenceException;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;

/**
 * Handles messages indicating that a device wants to register itself at the system and also generates
 * messages for each target that needs to know of this event and passes them to the OutgoingRouter.
 *
 * @author Christoph Fraedrich
 */
public class MasterUserConfigurationHandler extends AbstractMasterHandler {
    private static final String TAG = MasterUserConfigurationHandler.class.getSimpleName();

    @Override
    public void handle(Message.AddressedMessage message) {
        //FIXME //STOPSHIP will not work with new RoutingKeys (Niko, 2015-12-17)
        if (message.getPayload() instanceof UserDeviceInformationPayload) {
            sendUpdateToUserDevice(message.getFromID());
        } else if (message.getPayload() instanceof UserDeviceEditPayload) {
            executeUserDeviceEdit(message);
        } else if (message.getPayload() instanceof DeviceConnectedPayload) {
            sendUpdateToUserDevice(((DeviceConnectedPayload) message.getPayload()).deviceID);
        } else {
            sendErrorMessage(message); //wrong payload received
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{};
    }

    private void sendUpdateToUserDevice(DeviceID id) {
        Log.v(TAG, "sendUpdateToUser: " + id.getIDString());
        final Message userDeviceInformationMessage = new Message(generateUserDeviceInformationPayload());
        sendMessage(id, CoreConstants.RoutingKeys.APP_USERINFO_GET, userDeviceInformationMessage);
        final Message moduleInformationMessage = new Message(generateModuleInformationPayload());
        sendMessage(id, CoreConstants.RoutingKeys.GLOBAL_MODULES_UPDATE, moduleInformationMessage);
    }

    private void executeUserDeviceEdit(Message.AddressedMessage message) {
        UserDeviceEditPayload payload = (UserDeviceEditPayload) message.getPayload();

        switch (payload.getAction()) {
            case REMOVE_USERDEVICE:
                if (hasPermission(message.getFromID(), new Permission(
                        de.unipassau.isl.evs.ssh.core.Permission.DELETE_USER.toString(), ""))) {
                    removeUserDevice(payload);
                } else {
                    sendErrorMessage(message);
                }
                break;
            case EDIT_USERDEVICE:
                //TODO maybe refactor and unite both permissions?
                if (hasPermission(message.getFromID(), new Permission(
                        de.unipassau.isl.evs.ssh.core.Permission.CHANGE_USER_NAME.toString(), ""))
                        && hasPermission(message.getFromID(), new Permission(
                        de.unipassau.isl.evs.ssh.core.Permission.CHANGE_USER_GROUP.toString(), ""))) {
                    editUserDevice(message, payload);
                } else {
                    sendErrorMessage(message);
                }
                break;
            case GRANT_PERMISSION:
                if (hasPermission(message.getFromID(), new Permission(
                        de.unipassau.isl.evs.ssh.core.Permission.GRANT_USER_PERMISSION.toString(), ""))) {
                    grantPermission(message, payload);
                } else {
                    sendErrorMessage(message);
                }
                break;
            case REVOKE_PERMISSION:
                if (hasPermission(message.getFromID(), new Permission(
                        de.unipassau.isl.evs.ssh.core.Permission.WITHDRAW_USER_PERMISSION.toString(), ""))) {
                    revokePermission(payload);
                } else {
                    sendErrorMessage(message);
                }
                break;
        }
        sendUpdateToUserDevice(message.getFromID());
    }

    private void removeUserDevice(UserDeviceEditPayload payload) {
        UserDevice userDevice = payload.getUserDeviceToRemove();
        getContainer().require(UserManagementController.KEY).removeUserDevice(userDevice.getUserDeviceID());
    }

    private void editUserDevice(Message.AddressedMessage message, UserDeviceEditPayload payload) {
        Map<UserDevice, UserDevice> userToEdit = payload.getUserToEdit();
        //This is possible, because the map never has more then 2 values. It is used as a form of a tuple
        UserDevice toRemove = ((UserDevice[]) userToEdit.keySet().toArray())[0];
        UserDevice toAdd = userToEdit.get(toRemove);

        try {
            //TODO: for Refactor. New Method that updates a userdevice
            getContainer().require(UserManagementController.KEY).removeUserDevice(toRemove.getUserDeviceID());
            getContainer().require(UserManagementController.KEY).addUserDevice(toAdd);
        } catch (DatabaseControllerException e) {
            sendErrorMessage(message);
        }
    }

    private void grantPermission(Message.AddressedMessage message, UserDeviceEditPayload payload) {
        ImmutableListMultimap<UserDevice, Permission> userToGrantPermission = payload.getUserToGrantPermission();
        //This is possible, because the map never has more then 2 values. It is used as a form of a tuple

        UserDevice toGrant = ((UserDevice[]) userToGrantPermission.keySet().toArray())[0];
        for (Permission permission : userToGrantPermission.get(toGrant)) {
            try {
                getContainer().require(PermissionController.KEY).addUserPermission(
                        toGrant.getUserDeviceID(), permission);
            } catch (UnknownReferenceException e) {
                sendErrorMessage(message);
            }
        }
    }

    private void revokePermission(UserDeviceEditPayload payload) {
        ImmutableListMultimap<UserDevice, Permission> userToRevokePermission = payload.getUserToRevokePermission();
        //This is possible, because the map never has more then 2 values. It is used as a form of a tuple

        UserDevice toRevoke = ((UserDevice[]) userToRevokePermission.keySet().toArray())[0];
        for (Permission permission : userToRevokePermission.get(toRevoke)) {
            getContainer().require(PermissionController.KEY).removeUserPermission(
                    toRevoke.getUserDeviceID(), permission);
        }
    }

    private UserDeviceInformationPayload generateUserDeviceInformationPayload() {
        final PermissionController permissionController = requireComponent(PermissionController.KEY);
        final List<Group> groups = getContainer().require(UserManagementController.KEY).getGroups();
        final List<UserDevice> userDevices = getContainer().require(UserManagementController.KEY).getUserDevices();
        List<Permission> permissions = getContainer().require(PermissionController.KEY).getPermissions();

        ImmutableListMultimap<Group, UserDevice> groupDeviceMapping = Multimaps.index(userDevices,
                new Function<UserDevice, Group>() {
                    @Override
                    public Group apply(UserDevice input) {
                        for (Group group : groups) {
                            if (group.getName().equals(input.getInGroup())) {
                                return group;
                            }
                        }
                        return null;
                    }
                });

        ListMultimap<UserDevice, Permission> userHasPermissions = ArrayListMultimap.create();
        for (UserDevice userDevice : userDevices) {
            userHasPermissions.putAll(userDevice, permissionController.getPermissionsOfUserDevice(userDevice.getUserDeviceID()));
        }

        UserDeviceInformationPayload payload = new UserDeviceInformationPayload(
                ImmutableListMultimap.copyOf(userHasPermissions),
                ImmutableListMultimap.copyOf(groupDeviceMapping),
                permissions,
                groups
        );

        return payload;
    }

    private MessagePayload generateModuleInformationPayload() {
        SlaveController slaveController = getComponent(SlaveController.KEY);
        List<Slave> slaves = slaveController.getSlaves();
        ListMultimap<Slave, Module> modulesAtSlave = ArrayListMultimap.create();

        for (Slave slave : slaves) {
            modulesAtSlave.putAll(slave, slaveController.getModulesOfSlave(slave.getSlaveID()));
        }

        return new ModulesPayload(modulesAtSlave, slaves);
    }

}
