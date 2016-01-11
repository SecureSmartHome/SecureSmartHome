package de.unipassau.isl.evs.ssh.app.handler;

import android.support.annotation.NonNull;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DeleteDevicePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.GroupPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetGroupNamePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetGroupTemplatePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetPermissionPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetUserGroupPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetUserNamePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.UserDeviceInformationPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import io.netty.util.concurrent.Future;

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

    private final ListMultimap<UserDevice, Permission> usersToPermissions = ArrayListMultimap.create();
    private final ListMultimap<Group, UserDevice> groupToUserDevice = ArrayListMultimap.create();
    private final List<Permission> allPermissions = new LinkedList<>();
    private final List<Group> allGroups = new LinkedList<>();

    private final List<UserInfoListener> listeners = new LinkedList<>();

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

    private void fireUserInfoUpdated() {
        for (UserInfoListener listener : listeners) {
            listener.userInfoUpdated();
        }
    }

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

            ImmutableListMultimap<UserDevice, Permission> usersToPermissions = payload.getUsersToPermissions();
            if (usersToPermissions != null) {
                this.usersToPermissions.clear();
                this.usersToPermissions.putAll(usersToPermissions);
            }

            ImmutableListMultimap<Group, UserDevice> groupToUserDevice = payload.getGroupToUserDevice();
            if (groupToUserDevice != null) {
                this.groupToUserDevice.clear();
                this.groupToUserDevice.putAll(groupToUserDevice);
            }

            List<Permission> allPermissions = payload.getAllPermissions();
            if (allPermissions != null) {
                this.allPermissions.clear();
                this.allPermissions.addAll(allPermissions);
            }

            List<Group> allGroups = payload.getAllGroups();
            if (allGroups != null) {
                this.allGroups.clear();
                this.allGroups.addAll(allGroups);
            }

            fireUserInfoUpdated();
        } else {
            tryHandleResponse(message);
        }
        // invalidMessage is not called because handled by tryHandleResponse()
    }

    /**
     * Returns a list of all groups.
     *
     * @return a list of all groups
     */
    @NonNull
    public ImmutableList<Group> getAllGroups() {
        return ImmutableList.copyOf(allGroups);
    }

    /**
     * Returns a list of all UserDevices.
     *
     * @return a list of all UserDevices
     */
    @NonNull
    public ImmutableList<UserDevice> getAllUserDevices() {
        return ImmutableList.copyOf(usersToPermissions.keySet());
    }

    /**
     * Returns a list of all members of the given group.
     *
     * @param group the group
     * @return a list of all members
     */
    @NonNull
    public ImmutableList<UserDevice> getAllGroupMembers(Group group) {
        return ImmutableList.copyOf(groupToUserDevice.get(group));
    }

    /**
     * Returns a list of all permissions.
     *
     * @return a list of all permissions
     */
    @NonNull
    public ImmutableList<Permission> getAllPermissions() {
        return ImmutableList.copyOf(allPermissions);
    }

    /**
     * Returns a list of all permissions of the given user.
     *
     * @param user the user whose permissions are queried
     * @return a list of all permissions of the user
     */
    @NonNull
    public ImmutableList<Permission> getPermissionForUser(UserDevice user) {
        return ImmutableList.copyOf(usersToPermissions.get(user));
    }

    /**
     * Sends a message to master to add the given group.
     *
     * @param group the group to add
     */
    public Future<Void> addGroup(Group group) {
        Message message = new Message(new GroupPayload(group, GroupPayload.ACTION.CREATE));
        return newResponseFuture(sendMessageToMaster(MASTER_GROUP_ADD, message));
    }

    /**
     * Sends a message to master to remove the given group.
     *
     * @param group the group to remove
     */
    public Future<Void> removeGroup(Group group) {
        Message message = new Message(new GroupPayload(group, GroupPayload.ACTION.DELETE));
        return newResponseFuture(sendMessageToMaster(MASTER_GROUP_DELETE, message));
    }

    public Future<Void> setGroupName(Group group, String groupName) {
        Message message = new Message(new SetGroupNamePayload(group, groupName));
        return newResponseFuture(sendMessageToMaster(MASTER_GROUP_SET_NAME, message));
    }

    public Future<Void> setGroupTemplate(Group group, String templateName) {
        Message message = new Message(new SetGroupTemplatePayload(group, templateName));
        return newResponseFuture(sendMessageToMaster(MASTER_GROUP_SET_TEMPLATE, message));
    }

    public Future<Void> setUserName(DeviceID user, String username) {
        SetUserNamePayload payload = new SetUserNamePayload(user, username);
        return newResponseFuture(sendMessageToMaster(MASTER_USER_SET_NAME, new Message(payload)));
    }

    public Future<Void> setUserGroup(DeviceID user, String groupName) {
        SetUserGroupPayload payload = new SetUserGroupPayload(user, groupName);
        return newResponseFuture(sendMessageToMaster(MASTER_USER_SET_GROUP, new Message(payload)));
    }

    /**
     * Sends a message to master to grant the given permission to the given device.
     *
     * @param user       the user to grant the permission
     * @param permission the permission to grant
     */
    public Future<Void> grantPermission(DeviceID user, Permission permission) {
        SetPermissionPayload payload = new SetPermissionPayload(user, permission, SetPermissionPayload.Action.GRANT);
        return newResponseFuture(sendMessageToMaster(MASTER_PERMISSION_SET, new Message(payload)));
    }

    /**
     * Sends a message to master to remove the given permission from the given UserDevice.
     *
     * @param user       the user to remove the permission from
     * @param permission the permission to remove
     */
    public Future<Void> revokePermission(DeviceID user, Permission permission) {
        SetPermissionPayload payload = new SetPermissionPayload(user, permission, SetPermissionPayload.Action.REVOKE);
        return newResponseFuture(sendMessageToMaster(MASTER_PERMISSION_SET, new Message(payload)));
    }

    /**
     * Sends a message to master to remove the given UserDevice.
     *
     * @param user the user to remove
     */
    public Future<Void> removeUserDevice(DeviceID user) {
        DeleteDevicePayload payload = new DeleteDevicePayload(user);
        return newResponseFuture(sendMessageToMaster(MASTER_USER_DELETE, new Message(payload)));
    }

    /**
     * The listener to receive callbacks, when the user configuration changed.
     */
    public interface UserInfoListener {
        /**
         * Called when the user configuration changed.
         */
        void userInfoUpdated();
    }
}
