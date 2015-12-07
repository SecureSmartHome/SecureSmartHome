package de.unipassau.isl.evs.ssh.master.handler;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableListMultimap;
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
                removeUserDevice(payload);
                break;
            case EDIT_USERDEVICE:
                editUserDevice(message, payload);
                break;
            case GRANT_PERMISSION:
                grantPermission(message, payload);

                break;
            case REVOKE_PERMISSION:
                revokePermission(payload);
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
        for (Permission permission: userToGrantPermission.get(toGrant)) {
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
        for (Permission permission: userToRevokePermission.get(toRevoke)) {
            getContainer().require(PermissionController.KEY).removeUserPermission(
                    toRevoke.getUserDeviceID(), permission);
        }
    }

    private UserDeviceInformationPayload generatePayload() {
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

        ImmutableListMultimap<UserDevice, Permission> devicePermissionMapping = Multimaps.index(permissions,
                new Function<Permission, UserDevice>() {
                    @Override
                    public UserDevice apply(Permission input) {
                        for (UserDevice userDevice : userDevices) {
                            if (getContainer().require(PermissionController.KEY)
                                    .hasPermission(userDevice.getUserDeviceID(), input)) {
                                return userDevice;
                            }
                        }
                        return null;
                    }
                });


        UserDeviceInformationPayload payload = new UserDeviceInformationPayload(devicePermissionMapping,groupDeviceMapping, permissions);
        return payload;
    }
}
