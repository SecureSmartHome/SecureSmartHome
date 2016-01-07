package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Log;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

import java.util.LinkedList;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DeleteUserPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetPermissionPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetUserGroupPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetUserNamePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.UserDeviceInformationPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.AlreadyInUseException;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.UnknownReferenceException;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_USERINFO_UPDATE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DEVICE_CONNECTED;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_PERMISSION_SET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USERGROUP_SET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USERNAME_SET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USER_DELETE;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.CHANGE_USER_GROUP;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.CHANGE_USER_NAME;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.DELETE_USER;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.GRANT_USER_PERMISSION;

/**
 * Handles messages indicating that a device wants to register itself at the system and also generates
 * messages for each target that needs to know of this event and passes them to the OutgoingRouter.
 *
 * @author Christoph Fraedrich
 */
public class MasterUserConfigurationHandler extends AbstractMasterHandler {
    private static final String TAG = MasterUserConfigurationHandler.class.getSimpleName();

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                MASTER_DEVICE_CONNECTED,
                MASTER_PERMISSION_SET,
                MASTER_USERGROUP_SET,
                MASTER_USERNAME_SET,
                MASTER_USER_DELETE
        };
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_DEVICE_CONNECTED.matches(message)) {
            sendUpdateToUserDevice(MASTER_DEVICE_CONNECTED.getPayload(message).deviceID);
        } else if (MASTER_PERMISSION_SET.matches(message)) {
            setPermission(MASTER_PERMISSION_SET.getPayload(message), message);
        } else if (MASTER_USERGROUP_SET.matches(message)) {
            setUserGroup(MASTER_USERGROUP_SET.getPayload(message), message);
        } else if (MASTER_USERNAME_SET.matches(message)) {
            setUserName(MASTER_USERNAME_SET.getPayload(message), message);
        } else if (MASTER_USER_DELETE.matches(message)) {
            deleteUser(MASTER_USER_DELETE.getPayload(message), message);
        } else {
            invalidMessage(message);
        }
    }

    private void setUserName(SetUserNamePayload payload, Message.AddressedMessage original) {
        DeviceID fromID = original.getFromID();

        if (!hasPermission(fromID, CHANGE_USER_NAME, null)) {
            sendNoPermissionReply(original, CHANGE_USER_NAME);
            return;
        }

        try {
            requireComponent(UserManagementController.KEY).changeUserDeviceName(payload.getUser(), payload.getUsername());
            sendOnSuccess(original);
        } catch (AlreadyInUseException e) {
            sendReply(original, new Message(new ErrorPayload(e)));
        }
    }

    private void deleteUser(DeleteUserPayload payload, Message.AddressedMessage original) {
        DeviceID fromID = original.getFromID();

        if (!hasPermission(fromID, DELETE_USER, null)) {
            sendNoPermissionReply(original, DELETE_USER);
            return;
        }

        requireComponent(UserManagementController.KEY).removeUserDevice(payload.getUser());
        sendOnSuccess(original);
    }

    private void setUserGroup(SetUserGroupPayload payload, Message.AddressedMessage original) {
        DeviceID fromID = original.getFromID();

        if (!hasPermission(fromID, CHANGE_USER_GROUP, null)) {
            sendNoPermissionReply(original, CHANGE_USER_GROUP);
            return;
        }

        try {
            requireComponent(UserManagementController.KEY).changeGroupMembership(payload.getUser(), payload.getGroupName());
            sendOnSuccess(original);
        } catch (UnknownReferenceException e) {
            sendReply(original, new Message(new ErrorPayload(e)));
        }
    }

    private void setPermission(SetPermissionPayload payload, Message.AddressedMessage original) {
        PermissionController controller = requireComponent(PermissionController.KEY);
        DeviceID fromID = original.getFromID();

        if (!hasPermission(fromID, GRANT_USER_PERMISSION, null)) {
            sendNoPermissionReply(original, GRANT_USER_PERMISSION);
            return;
        }

        de.unipassau.isl.evs.ssh.core.sec.Permission permission = payload.getPermission().getPermission();
        String moduleName = payload.getPermission().getModuleName();

        boolean success = false;
        if (payload.getAction().equals(SetPermissionPayload.Action.GRANT)) {
            try {
                controller.addUserPermission(payload.getUser(), permission, moduleName);
                success = true;
            } catch (UnknownReferenceException e) {
                sendReply(original, new Message(new ErrorPayload(e)));
            }
        } else if (payload.getAction().equals(SetPermissionPayload.Action.REVOKE)) {
            controller.removeUserPermission(payload.getUser(), permission, moduleName);
            success = true;
        }

        if (success) {
            sendOnSuccess(original);
        }
    }

    private void sendOnSuccess(Message.AddressedMessage original) {
        sendReply(original, new Message());
        broadcastUserConfigurationUpdated();
    }

    private void broadcastUserConfigurationUpdated() {
        List<UserDevice> userDevices = requireComponent(UserManagementController.KEY).getUserDevices();
        for (UserDevice userDevice : userDevices) {
            sendUpdateToUserDevice(userDevice.getUserDeviceID());
        }
    }

    private void sendUpdateToUserDevice(DeviceID id) {
        Log.v(TAG, "sendUpdateToUser: " + id.getIDString());

        if (!isSlave(id)) {
            final Message userDeviceInformationMessage = new Message(generateUserDeviceInformationPayload());
            sendMessage(id, APP_USERINFO_UPDATE, userDeviceInformationMessage);
        }
    }

    private UserDeviceInformationPayload generateUserDeviceInformationPayload() {
        final PermissionController permissionController = requireComponent(PermissionController.KEY);
        final List<Group> groups;
        final List<UserDevice> userDevices;
        List<Permission> permissions;
        if (getContainer() != null) {
            groups = getContainer().require(UserManagementController.KEY).getGroups();
            userDevices = getContainer().require(UserManagementController.KEY).getUserDevices();
            permissions = getContainer().require(PermissionController.KEY).getPermissions();
        } else {
            groups = new LinkedList<>();
            userDevices = new LinkedList<>();
            permissions = new LinkedList<>();
        }

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

        return new UserDeviceInformationPayload(
                ImmutableListMultimap.copyOf(userHasPermissions),
                ImmutableListMultimap.copyOf(groupDeviceMapping),
                permissions,
                groups
        );
    }
}
