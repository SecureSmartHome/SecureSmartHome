package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Log;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DeviceConnectedPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.UserDeviceEditPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.UserDeviceInformationPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.DatabaseContract;
import de.unipassau.isl.evs.ssh.master.database.DatabaseControllerException;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.UnknownReferenceException;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;

import java.util.List;
import java.util.Map;

/**
 * Handles messages indicating that a device wants to register itself at the system and also generates
 * messages for each target that needs to know of this event and passes them to the OutgoingRouter.
 *
 * @author Chris
 */
public class MasterUserConfigurationHandler extends AbstractMasterHandler {

    private static final String TAG = MasterUserConfigurationHandler.class.getSimpleName();

    @Override
    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof UserDeviceInformationPayload) {
            sendUserInfoUpdate(message);
        } else if (message.getPayload() instanceof UserDeviceEditPayload) {
            executeUserDeviceEdit(message);
        } else if (message.getPayload() instanceof DeviceConnectedPayload) {
            sendUpdateToUserDevice(((DeviceConnectedPayload) message.getPayload()).deviceID);
        } else {
            sendErrorMessage(message); //wrong payload received
        }
    }

    private void sendUpdateToUserDevice(DeviceID id) {
        Log.v(TAG, "sendUpdateToUser: " + id.getIDString());
        final Message messageToSend = new Message(generatePayload());
        sendMessage(id, CoreConstants.RoutingKeys.APP_USERINFO_GET, messageToSend);
    }

    private void sendUserInfoUpdate(Message.AddressedMessage message) {
        final Message messageToSend = new Message(generatePayload());
        messageToSend.putHeader(Message.HEADER_REPLY_TO_KEY, message.getRoutingKey());
        sendMessage(message.getFromID(), CoreConstants.RoutingKeys.APP_USERINFO_GET, messageToSend);
    }

    private void executeUserDeviceEdit(Message.AddressedMessage message) {
        UserDeviceEditPayload payload = (UserDeviceEditPayload) message.getPayload();

        switch (payload.getAction()) {
            case REMOVE_USERDEVICE:
                if (hasPermission(message.getFromID(), new Permission(
                        DatabaseContract.Permission.Values.DELETE_USER, ""))) {
                    removeUserDevice(payload);
                } else {
                    sendErrorMessage(message);
                }
                break;
            case EDIT_USERDEVICE:
                //TODO maybe refactor and unite both permissions?
                if (hasPermission(message.getFromID(), new Permission(
                        DatabaseContract.Permission.Values.CHANGE_USER_NAME, ""))
                        && hasPermission(message.getFromID(), new Permission(
                        DatabaseContract.Permission.Values.CHANGE_USER_GROUP, ""))) {
                    editUserDevice(message, payload);
                } else {
                    sendErrorMessage(message);
                }
                break;
            case GRANT_PERMISSION:
                if (hasPermission(message.getFromID(), new Permission(
                        DatabaseContract.Permission.Values.GRANT_USER_RIGHT, ""))) {
                    grantPermission(message, payload);
                } else {
                    sendErrorMessage(message);
                }
                break;
            case REVOKE_PERMISSION:
                if (hasPermission(message.getFromID(), new Permission(
                        DatabaseContract.Permission.Values.WITHDRAW_USER_RIGHT, ""))) {
                    revokePermission(payload);
                } else {
                    sendErrorMessage(message);
                }
                break;
        }
        sendUserInfoUpdate(message);
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

    private UserDeviceInformationPayload generatePayload() {
        final PermissionController permissionController = requireComponent(PermissionController.KEY);
        final List<Group> groups = getContainer().require(UserManagementController.KEY).getGroups();
        final List<UserDevice> userDevices = getContainer().require(UserManagementController.KEY).getUserDevices();
        List<Permission> permissions = getContainer().require(PermissionController.KEY).getPermissions();

        ListMultimap<Group, UserDevice> groupDeviceMapping = ArrayListMultimap.create();
        for (final Group group : groups) {
            Predicate<UserDevice> predicate = new Predicate<UserDevice>() {
                @Override
                public boolean apply(UserDevice userDevice) {
                    return userDevice.getInGroup().equals(group.getName());
                }
            };
            groupDeviceMapping.putAll(group, Iterables.filter(userDevices, predicate));
        }

        ListMultimap<UserDevice, Permission> userHasPermissions = ArrayListMultimap.create();
        for (UserDevice userDevice : userDevices) {
            userHasPermissions.putAll(userDevice, permissionController.getPermissionsOfUserDevice(userDevice.getUserDeviceID()));
        }


        UserDeviceInformationPayload payload = new UserDeviceInformationPayload(
                ImmutableListMultimap.copyOf(userHasPermissions),
                ImmutableListMultimap.copyOf(groupDeviceMapping),
                permissions
        );

        return payload;
    }
}
