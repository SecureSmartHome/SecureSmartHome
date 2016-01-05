package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Log;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

import java.util.List;
import java.util.Map;

import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
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

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_USERINFO_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.GLOBAL_MODULES_UPDATE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DEVICE_CONNECTED;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USERINFO_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USERINFO_SET;

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
        if (MASTER_USERINFO_GET.matches(message)) {
            sendUpdateToUserDevice(message.getFromID());
        } else if (MASTER_USERINFO_SET.matches(message)) {
            executeUserDeviceEdit(message, MASTER_USERINFO_SET.getPayload(message));
        } else if (MASTER_DEVICE_CONNECTED.matches(message)) {
            sendUpdateToUserDevice(MASTER_DEVICE_CONNECTED.getPayload(message).deviceID);
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_DEVICE_CONNECTED, MASTER_USERINFO_GET, MASTER_USERINFO_SET};
    }

    private void sendUpdateToUserDevice(DeviceID id) {
        Log.v(TAG, "sendUpdateToUser: " + id.getIDString());

        if (!isSlave(id)) {
            final Message userDeviceInformationMessage = new Message(generateUserDeviceInformationPayload());
            sendMessage(id, APP_USERINFO_GET, userDeviceInformationMessage);
        }

        //TODO this is probably not the correct location to send module information. This has to be done by the MasterModuleHandler
        final Message moduleInformationMessage = new Message(generateModuleInformationPayload());
        sendMessage(id, GLOBAL_MODULES_UPDATE, moduleInformationMessage);
    }

    private void executeUserDeviceEdit(Message.AddressedMessage message, UserDeviceEditPayload payload) {
        switch (payload.getAction()) {
            case REMOVE_USERDEVICE:
                if (hasPermission(message.getFromID(), new Permission(
                        de.unipassau.isl.evs.ssh.core.sec.Permission.DELETE_USER.toString(), ""))) {
                    removeUserDevice(payload);
                } else {
                    //HANDLE
                    sendErrorMessage(message);
                }
                break;
            case EDIT_USERDEVICE:
                //TODO maybe refactor and unite both permissions?
                if (hasPermission(message.getFromID(), new Permission(
                        de.unipassau.isl.evs.ssh.core.sec.Permission.CHANGE_USER_NAME.toString(), ""))
                        && hasPermission(message.getFromID(), new Permission(
                        de.unipassau.isl.evs.ssh.core.sec.Permission.CHANGE_USER_GROUP.toString(), ""))) {
                    editUserDevice(message, payload);
                } else {
                    //HANDLE
                    sendErrorMessage(message);
                }
                break;
            case GRANT_PERMISSION:
                if (hasPermission(message.getFromID(), new Permission(
                        de.unipassau.isl.evs.ssh.core.sec.Permission.GRANT_USER_PERMISSION.toString(), ""))) {
                    grantPermission(message, payload);
                } else {
                    //HANDLE
                    sendErrorMessage(message);
                }
                break;
            case REVOKE_PERMISSION:
                if (hasPermission(message.getFromID(), new Permission(
                        de.unipassau.isl.evs.ssh.core.sec.Permission.WITHDRAW_USER_PERMISSION.toString(), ""))) {
                    revokePermission(payload);
                } else {
                    //HANDLE
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
            //HANDLE
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
                //HANDLE
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
