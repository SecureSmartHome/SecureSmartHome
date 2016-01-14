package de.unipassau.isl.evs.ssh.app.handler;

import android.support.annotation.NonNull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.PermissionDTO;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DeleteDevicePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.GroupPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetGroupNamePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetGroupTemplatePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetPermissionPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetUserGroupPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetUserNamePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.UserDeviceInformationPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_USERINFO_UPDATE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_GROUP_ADD;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_GROUP_ADD_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_GROUP_ADD_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_GROUP_DELETE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_GROUP_DELETE_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_GROUP_DELETE_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_GROUP_SET_NAME;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_GROUP_SET_NAME_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_GROUP_SET_NAME_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_GROUP_SET_TEMPLATE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_GROUP_SET_TEMPLATE_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_GROUP_SET_TEMPLATE_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_PERMISSION_SET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_PERMISSION_SET_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_PERMISSION_SET_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USERNAME_SET_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USERNAME_SET_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USER_DELETE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USER_DELETE_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USER_DELETE_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USER_SET_GROUP;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USER_SET_GROUP_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USER_SET_GROUP_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USER_SET_NAME;

/**
 * The AppUserConfigurationHandler handles the messaging that is needed to provide user and group
 * information on the app.
 *
 * @author Wolfgang Popp
 */
public class AppUserConfigurationHandler extends AbstractAppHandler implements Component {
    public static final Key<AppUserConfigurationHandler> KEY = new Key<>(AppUserConfigurationHandler.class);

    private final SetMultimap<UserDevice, PermissionDTO> usersToPermissions = HashMultimap.create();
    private final SetMultimap<Group, UserDevice> groupToUserDevice = HashMultimap.create();
    private final Set<PermissionDTO> allPermissions = new HashSet<>();
    private final Set<Group> allGroups = new HashSet<>();
    private final Set<String> allTemplates = new HashSet<>();

    private final List<UserInfoListener> listeners = new LinkedList<>();

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                APP_USERINFO_UPDATE,
                MASTER_PERMISSION_SET_ERROR,
                MASTER_PERMISSION_SET_REPLY,
                MASTER_USERNAME_SET_ERROR,
                MASTER_USERNAME_SET_REPLY,
                MASTER_USER_SET_GROUP_ERROR,
                MASTER_USER_SET_GROUP_REPLY,
                MASTER_USER_DELETE_ERROR,
                MASTER_USER_DELETE_REPLY,
                MASTER_GROUP_ADD_ERROR,
                MASTER_GROUP_ADD_REPLY,
                MASTER_GROUP_DELETE_ERROR,
                MASTER_GROUP_DELETE_REPLY,
                MASTER_GROUP_SET_NAME_ERROR,
                MASTER_GROUP_SET_NAME_REPLY,
                MASTER_GROUP_SET_TEMPLATE_ERROR,
                MASTER_GROUP_SET_TEMPLATE_REPLY
        };
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (APP_USERINFO_UPDATE.matches(message)) {
            UserDeviceInformationPayload payload = APP_USERINFO_UPDATE.getPayload(message);

            ListMultimap<UserDevice, PermissionDTO> usersToPermissions = payload.getUsersToPermissions();
            if (usersToPermissions != null) {
                this.usersToPermissions.clear();
                this.usersToPermissions.putAll(usersToPermissions);
            }

            ListMultimap<Group, UserDevice> groupToUserDevice = payload.getGroupToUserDevice();
            if (groupToUserDevice != null) {
                this.groupToUserDevice.clear();
                this.groupToUserDevice.putAll(groupToUserDevice);
            }

            List<PermissionDTO> allPermissions = payload.getAllPermissions();
            if (allPermissions != null) {
                this.allPermissions.clear();
                this.allPermissions.addAll(allPermissions);
            }

            List<Group> allGroups = payload.getAllGroups();
            if (allGroups != null) {
                this.allGroups.clear();
                this.allGroups.addAll(allGroups);
            }

            List<String> allTemplates = payload.getTemplates();
            if (allTemplates != null) {
                this.allTemplates.clear();
                this.allTemplates.addAll(allTemplates);
            }

            fireUserInfoUpdated(new UserConfigurationEvent());
        } else {
            tryHandleResponse(message);
        }
        // invalidMessage is not called because handled by tryHandleResponse()
    }

    /**
     * Adds the given listener to this handler.
     *
     * @param listener the listener to add
     */
    public void addUserInfoListener(AppUserConfigurationHandler.UserInfoListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the given listener from this handler.
     *
     * @param listener the listener to remove
     */
    public void removeUserInfoListener(AppUserConfigurationHandler.UserInfoListener listener) {
        listeners.remove(listener);
    }

    private void fireUserInfoUpdated(UserConfigurationEvent event) {
        for (UserInfoListener listener : listeners) {
            listener.userInfoUpdated(event);
        }
    }

    /**
     * Returns a list of all groups.
     *
     * @return a list of all groups
     */
    @NonNull
    public Set<Group> getAllGroups() {
        return Collections.unmodifiableSet(allGroups);
    }

    /**
     * Returns a list of all UserDevices.
     *
     * @return a list of all UserDevices
     */
    @NonNull
    public Set<UserDevice> getAllUserDevices() {
        return Collections.unmodifiableSet(usersToPermissions.keySet());
    }

    /**
     * Returns a list of all members of the given group.
     *
     * @param group the group
     * @return a list of all members
     */
    @NonNull
    public Set<UserDevice> getAllGroupMembers(Group group) {
        return Collections.unmodifiableSet(groupToUserDevice.get(group));
    }

    /**
     * Returns a list of all permissions.
     *
     * @return a list of all permissions
     */
    @NonNull
    public Set<PermissionDTO> getAllPermissions() {
        return Collections.unmodifiableSet(allPermissions);
    }

    /**
     * Returns a list of all permissions of the given user.
     *
     * @param user the user whose permissions are queried
     * @return a list of all permissions of the user
     */
    @NonNull
    public Set<PermissionDTO> getPermissionForUser(UserDevice user) {
        return Collections.unmodifiableSet(usersToPermissions.get(user));
    }

    /**
     * Returns a list of all permissions of the given device.
     *
     * @param userDeviceID DeviceID associated with the user.
     * @return a list of all permissions of the user
     */
    @NonNull
    public Set<PermissionDTO> getPermissionForUser(DeviceID userDeviceID) {
        for (UserDevice device : usersToPermissions.keySet()) {
            if (userDeviceID.equals(device.getUserDeviceID())) {
                return getPermissionForUser(device);
            }
        }
        return Collections.emptySet();
    }

    /**
     * Returns a list of all templates.
     *
     * @return a list of all templates
     */
    public Set<String> getAllTemplates() {
        return Collections.unmodifiableSet(allTemplates);
    }

    /**
     * Returns whether a given user has a given PermissionDTO.
     *
     * @param userDeviceID DeviceID associated with the user.
     * @param permission   PermissionDTO to check.
     * @return true if has permissions otherwise false.
     */
    public boolean hasPermission(DeviceID userDeviceID, PermissionDTO permission) {
        for (UserDevice device : usersToPermissions.keySet()) {
            if (userDeviceID.equals(device.getUserDeviceID())) {
                return usersToPermissions.get(device).contains(permission);
            }
        }
        return false;
    }

    private void sendUserConfigMessage(Message message, RoutingKey<? extends MessagePayload> key, final UserConfigurationEvent.EventType eventType) {
        final Future<Void> future = newResponseFuture(sendMessageToMaster(key, message));
        future.addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                final boolean success = future.isSuccess();
                fireUserInfoUpdated(new UserConfigurationEvent(eventType, success));
            }
        });
    }

    /**
     * Sends a message to master to add the given group.
     *
     * @param group the group to add
     */
    public void addGroup(Group group) {
        Message message = new Message(new GroupPayload(group, GroupPayload.ACTION.CREATE));
        sendUserConfigMessage(message, MASTER_GROUP_ADD, UserConfigurationEvent.EventType.GROUP_ADD);
    }

    /**
     * Sends a message to master to remove the given group.
     *
     * @param group the group to remove
     */
    public void removeGroup(Group group) {
        Message message = new Message(new GroupPayload(group, GroupPayload.ACTION.DELETE));
        sendUserConfigMessage(message, MASTER_GROUP_DELETE, UserConfigurationEvent.EventType.GROUP_DELETE);
    }

    /**
     * Sends a message to master to set the name of the given group.
     *
     * @param group     the group to modify
     * @param groupName the name to set
     */
    public void setGroupName(Group group, String groupName) {
        Message message = new Message(new SetGroupNamePayload(group, groupName));
        sendUserConfigMessage(message, MASTER_GROUP_SET_NAME, UserConfigurationEvent.EventType.GROUP_SET_NAME);
    }

    /**
     * Sends a message to master to set the template of the given group.
     *
     * @param group        the group to modify
     * @param templateName the template to set
     */
    public void setGroupTemplate(Group group, String templateName) {
        Message message = new Message(new SetGroupTemplatePayload(group, templateName));
        sendUserConfigMessage(message, MASTER_GROUP_SET_TEMPLATE, UserConfigurationEvent.EventType.GROUP_SET_TEMPLATE);
    }

    /**
     * Sends a message to master to set the name of the given user.
     *
     * @param user     the user to modify
     * @param username the name to set
     */
    public void setUserName(DeviceID user, String username) {
        Message message = new Message(new SetUserNamePayload(user, username));
        sendUserConfigMessage(message, MASTER_USER_SET_NAME, UserConfigurationEvent.EventType.USERNAME_SET);
    }

    /**
     * Sends a message to master to set group of the given user.
     *
     * @param user      the user to modify
     * @param groupName the group to set
     */
    public void setUserGroup(DeviceID user, String groupName) {
        Message message = new Message(new SetUserGroupPayload(user, groupName));
        sendUserConfigMessage(message, MASTER_USER_SET_GROUP, UserConfigurationEvent.EventType.USER_SET_GROUP);
    }

    /**
     * Sends a message to master to grant the given permission to the given device.
     *
     * @param user       the user to grant the permission
     * @param permission the permission to grant
     */
    public void grantPermission(DeviceID user, PermissionDTO permission) {
        Message message = new Message(new SetPermissionPayload(user, permission, SetPermissionPayload.Action.GRANT));
        sendUserConfigMessage(message, MASTER_PERMISSION_SET, UserConfigurationEvent.EventType.PERMISSION_GRANT);
    }

    /**
     * Sends a message to master to remove the given permission from the given UserDevice.
     *
     * @param user       the user to remove the permission from
     * @param permission the permission to remove
     */
    public void revokePermission(DeviceID user, PermissionDTO permission) {
        Message message = new Message(new SetPermissionPayload(user, permission, SetPermissionPayload.Action.REVOKE));
        sendUserConfigMessage(message, MASTER_PERMISSION_SET, UserConfigurationEvent.EventType.PERMISSION_REVOKE);
    }

    /**
     * Sends a message to master to remove the given UserDevice.
     *
     * @param user the user to remove
     */
    public void removeUserDevice(DeviceID user) {
        Message message = new Message(new DeleteDevicePayload(user));
        sendUserConfigMessage(message, MASTER_USER_DELETE, UserConfigurationEvent.EventType.USER_DELETE);
    }

    /**
     * The listener to receive callbacks, when the user configuration changed.
     */
    public interface UserInfoListener {
        /**
         * Called when the user configuration changed.
         *
         * @param event the event that triggered the invocation of this method
         */
        void userInfoUpdated(UserConfigurationEvent event);
    }
}
