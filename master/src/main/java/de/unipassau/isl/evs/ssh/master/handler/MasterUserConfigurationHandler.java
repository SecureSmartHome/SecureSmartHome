package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DeleteDevicePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.GroupPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetGroupNamePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetGroupTemplatePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetPermissionPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetUserGroupPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetUserNamePayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.AlreadyInUseException;
import de.unipassau.isl.evs.ssh.master.database.DatabaseControllerException;
import de.unipassau.isl.evs.ssh.master.database.IsReferencedException;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.UnknownReferenceException;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;
import de.unipassau.isl.evs.ssh.master.network.UserConfigurationBroadcaster;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DEVICE_CONNECTED;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_GROUP_ADD;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_GROUP_DELETE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_GROUP_SET_NAME;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_GROUP_SET_TEMPLATE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_PERMISSION_SET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USER_DELETE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USER_SET_GROUP;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USER_SET_NAME;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.ADD_GROUP;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.CHANGE_GROUP_NAME;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.CHANGE_GROUP_TEMPLATE;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.CHANGE_USER_GROUP;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.CHANGE_USER_NAME;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.DELETE_GROUP;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.DELETE_USER;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.MODIFY_USER_PERMISSION;

/**
 * Handles messages indicating that a device wants to register itself at the system and also generates
 * messages for each target that needs to know of this event and passes them to the OutgoingRouter.
 *
 * @author Christoph Fraedrich
 * @author Wolfgang Popp
 */
public class MasterUserConfigurationHandler extends AbstractMasterHandler {

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                MASTER_DEVICE_CONNECTED,
                MASTER_PERMISSION_SET,
                MASTER_USER_SET_GROUP,
                MASTER_USER_SET_NAME,
                MASTER_USER_DELETE,
                MASTER_GROUP_ADD,
                MASTER_GROUP_DELETE,
                MASTER_GROUP_SET_NAME,
                MASTER_GROUP_SET_TEMPLATE
        };
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_DEVICE_CONNECTED.matches(message)) {
            final UserConfigurationBroadcaster broadcaster = requireComponent(UserConfigurationBroadcaster.KEY);
            broadcaster.updateClient(MASTER_DEVICE_CONNECTED.getPayload(message).deviceID);
        } else if (MASTER_PERMISSION_SET.matches(message)) {
            setPermission(MASTER_PERMISSION_SET.getPayload(message), message);
        } else if (MASTER_USER_SET_GROUP.matches(message)) {
            setUserGroup(MASTER_USER_SET_GROUP.getPayload(message), message);
        } else if (MASTER_USER_SET_NAME.matches(message)) {
            setUserName(MASTER_USER_SET_NAME.getPayload(message), message);
        } else if (MASTER_USER_DELETE.matches(message)) {
            deleteUser(MASTER_USER_DELETE.getPayload(message), message);
        } else if (MASTER_GROUP_ADD.matches(message)) {
            addGroup(MASTER_GROUP_ADD.getPayload(message), message);
        } else if (MASTER_GROUP_DELETE.matches(message)) {
            deleteGroup(MASTER_GROUP_DELETE.getPayload(message), message);
        } else if (MASTER_GROUP_SET_NAME.matches(message)) {
            setGroupName(MASTER_GROUP_SET_NAME.getPayload(message), message);
        } else if (MASTER_GROUP_SET_TEMPLATE.matches(message)) {
            setGroupTemplate(MASTER_GROUP_SET_TEMPLATE.getPayload(message), message);
        } else {
            invalidMessage(message);
        }
    }

    private void addGroup(GroupPayload payload, Message.AddressedMessage original) {
        DeviceID fromID = original.getFromID();

        if (!hasPermission(fromID, ADD_GROUP)) {
            sendNoPermissionReply(original, ADD_GROUP);
            return;
        }

        try {
            requireComponent(UserManagementController.KEY).addGroup(payload.getGroup());
            sendOnSuccess(original);
        } catch (DatabaseControllerException e) {
            sendError(original, e);
        }
    }

    private void deleteGroup(GroupPayload payload, Message.AddressedMessage original) {
        DeviceID fromID = original.getFromID();

        if (!hasPermission(fromID, DELETE_GROUP)) {
            sendNoPermissionReply(original, DELETE_GROUP);
            return;
        }

        try {
            requireComponent(UserManagementController.KEY).removeGroup(payload.getGroup().getName());
            sendOnSuccess(original);
        } catch (IsReferencedException e) {
            sendError(original, e);
        }
    }

    private void setGroupName(SetGroupNamePayload payload, Message.AddressedMessage original) {
        DeviceID fromID = original.getFromID();

        if (!hasPermission(fromID, CHANGE_GROUP_NAME)) {
            sendNoPermissionReply(original, CHANGE_GROUP_NAME);
            return;
        }

        try {
            requireComponent(UserManagementController.KEY).changeGroupName(payload.getGroup().getName(), payload.getNewName());
            sendOnSuccess(original);
        } catch (AlreadyInUseException e) {
            sendError(original, e);
        }
    }

    private void setGroupTemplate(SetGroupTemplatePayload payload, Message.AddressedMessage original) {
        DeviceID fromID = original.getFromID();

        if (!hasPermission(fromID, CHANGE_GROUP_TEMPLATE)) {
            sendNoPermissionReply(original, CHANGE_GROUP_TEMPLATE);
            return;
        }

        try {
            requireComponent(UserManagementController.KEY).changeTemplateOfGroup(payload.getGroup().getName(), payload.getTemplateName());
            sendOnSuccess(original);
        } catch (UnknownReferenceException e) {
            sendError(original, e);
        }
    }

    private void setUserName(SetUserNamePayload payload, Message.AddressedMessage original) {
        DeviceID fromID = original.getFromID();

        if (!hasPermission(fromID, CHANGE_USER_NAME)) {
            sendNoPermissionReply(original, CHANGE_USER_NAME);
            return;
        }

        try {
            requireComponent(UserManagementController.KEY).changeUserDeviceName(payload.getUser(), payload.getUsername());
            sendOnSuccess(original);
        } catch (AlreadyInUseException e) {
            sendError(original, e);
        }
    }

    private void deleteUser(DeleteDevicePayload payload, Message.AddressedMessage original) {
        DeviceID fromID = original.getFromID();

        if (!hasPermission(fromID, DELETE_USER)) {
            sendNoPermissionReply(original, DELETE_USER);
            return;
        }

        requireComponent(UserManagementController.KEY).removeUserDevice(payload.getUser());
        sendOnSuccess(original);
    }

    private void setUserGroup(SetUserGroupPayload payload, Message.AddressedMessage original) {
        DeviceID fromID = original.getFromID();

        if (!hasPermission(fromID, CHANGE_USER_GROUP)) {
            sendNoPermissionReply(original, CHANGE_USER_GROUP);
            return;
        }

        try {
            requireComponent(UserManagementController.KEY).changeGroupMembership(payload.getUser(), payload.getGroupName());
            sendOnSuccess(original);
        } catch (UnknownReferenceException e) {
            sendError(original, e);
        }
    }

    private void setPermission(SetPermissionPayload payload, Message.AddressedMessage original) {
        PermissionController controller = requireComponent(PermissionController.KEY);
        DeviceID fromID = original.getFromID();

        if (!hasPermission(fromID, MODIFY_USER_PERMISSION)) {
            sendNoPermissionReply(original, MODIFY_USER_PERMISSION);
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
                sendError(original, e);
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
        requireComponent(UserConfigurationBroadcaster.KEY).updateAllClients();
    }

    private void sendError(Message.AddressedMessage original, Exception e) {
        sendReply(original, new Message(new ErrorPayload(e)));
    }
}
