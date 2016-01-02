package de.unipassau.isl.evs.ssh.app.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Lists;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys;
import de.unipassau.isl.evs.ssh.core.messaging.payload.GroupEditPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.UserDeviceEditPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.UserDeviceInformationPayload;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_USERINFO_GET;

/**
 * The AppUserConfigurationHandler handles the messaging that is needed to provide user and group
 * information on the app.
 *
 * @author Wolfgang Popp
 */
public class AppUserConfigurationHandler extends AbstractMessageHandler implements Component {
    public static final Key<AppUserConfigurationHandler> KEY = new Key<>(AppUserConfigurationHandler.class);

    private ImmutableListMultimap<UserDevice, Permission> usersToPermissions;
    private ImmutableListMultimap<Group, UserDevice> groupToUserDevice;
    private List<Permission> allPermissions;
    private List<Group> allGroups;

    private List<UserInfoListener> listeners = new LinkedList<>();

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
        return new RoutingKey[]{APP_USERINFO_GET};
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (APP_USERINFO_GET.matches(message)) {
            UserDeviceInformationPayload payload = APP_USERINFO_GET.getPayload(message);
            this.usersToPermissions = payload.getUsersToPermissions();
            this.groupToUserDevice = payload.getGroupToUserDevice();
            this.allPermissions = payload.getAllPermissions();
            this.allGroups = payload.getAllGroups();
            fireUserInfoUpdated();
        } else {
            invalidMessage(message);
        }
    }

    private void update() {
        UserDeviceInformationPayload payload = new UserDeviceInformationPayload();
        OutgoingRouter router = requireComponent(OutgoingRouter.KEY);

        Message message = new Message(payload);

        message.putHeader(Message.HEADER_REPLY_TO_KEY, APP_USERINFO_GET.getKey());
        router.sendMessageToMaster(RoutingKeys.MASTER_USERINFO_GET, message);
    }

    /**
     * Returns a list of all groups.
     *
     * @return a list of all groups or null if no groups are available
     */
    public List<Group> getAllGroups() {
        if (allGroups == null || allGroups.size() < 1) {
            return null;
        }
        return ImmutableList.copyOf(allGroups);
    }

    /**
     * Returns a list of all UserDevices.
     *
     * @return a list of all UserDevices or null if no groups are available
     */
    public List<UserDevice> getAllUserDevices() {
        if (usersToPermissions == null || usersToPermissions.size() < 1) {
            return null;
        }
        return Lists.newArrayList(usersToPermissions.keySet());
    }

    /**
     * Returns a list of all members of the given group.
     *
     * @param group the group
     * @return a list of all members or null
     */
    public List<UserDevice> getAllGroupMembers(Group group) {
        if (groupToUserDevice == null || groupToUserDevice.size() < 1) {
            return null;
        }
        return groupToUserDevice.get(group);
    }

    /**
     * Returns a list of all permissions.
     *
     * @return a list of all permissions or null
     */
    public List<Permission> getAllPermissions() {
        if (allPermissions == null || allPermissions.size() < 1) {
            return null;
        }
        return ImmutableList.copyOf(allPermissions);
    }

    /**
     * Returns a list of all permissions of the given user.
     *
     * @param user the user whose permissions are queried
     * @return a list of all permissions of the user or null
     */
    public List<Permission> getPermissionForUser(UserDevice user) {
        if (usersToPermissions == null || usersToPermissions.size() < 1) {
            return null;
        }
        return usersToPermissions.get(user);
    }

    private void sendEditMessage(MessagePayload payload) {
        Message message = new Message(payload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, APP_USERINFO_GET.getKey());
        OutgoingRouter router = requireComponent(OutgoingRouter.KEY);
        router.sendMessageToMaster(RoutingKeys.MASTER_USERINFO_SET, message);
    }

    /**
     * Sends a message to master to add the given group.
     *
     * @param group the group to add
     */
    public void addGroup(Group group) {
        GroupEditPayload payload = GroupEditPayload.newAddGroupPayload(group);
        sendEditMessage(payload);
    }

    /**
     * Sends a message to master to remove the given group.
     *
     * @param group the group to remove
     */
    public void removeGroup(Group group) {
        GroupEditPayload payload = GroupEditPayload.newRemoveGroupPayload(group);
        sendEditMessage(payload);
    }

    /**
     * Sends a message to master to update the given {@code oldGroup} to the given {@code newGroup}
     *
     * @param oldGroup the group to edit
     * @param newGroup the new group information
     */
    public void editGroup(Group oldGroup, Group newGroup) {
        GroupEditPayload payload = GroupEditPayload.newEditGroupPayload(oldGroup, newGroup);
        sendEditMessage(payload);
    }

    /**
     * Sends a message to master to grant the given permission to the given device.
     *
     * @param device     the UserDevice to grant the permission
     * @param permission the permission to grant
     */
    public void grantPermission(UserDevice device, Permission permission) {
        UserDeviceEditPayload payload = UserDeviceEditPayload.newGrantPermissionPayload(device, permission);
        sendEditMessage(payload);
    }

    /**
     * Sends a message to master to remove the given permission from the given UserDevice.
     *
     * @param device     the UserDevice to remove the permission from
     * @param permission the permission to remove
     */
    public void revokePermission(UserDevice device, Permission permission) {
        UserDeviceEditPayload payload = UserDeviceEditPayload.newRevokePermissionPayload(device, permission);
        sendEditMessage(payload);
    }

    /**
     * Sends a message to master to edit the given user device.
     *
     * @param oldDevice the UserDevice that gets edited
     * @param newDevice the new configuration for {@code oldUserDevice}
     */
    public void editUserDevice(UserDevice oldDevice, UserDevice newDevice) {
        UserDeviceEditPayload payload = UserDeviceEditPayload.newEditUserPayload(oldDevice, newDevice);
        sendEditMessage(payload);
    }

    /**
     * Sends a message to master to remove the given UserDevice.
     *
     * @param userDevice the UserDevice to remove
     */
    public void removeUserDevice(UserDevice userDevice) {
        UserDeviceEditPayload payload = UserDeviceEditPayload.newRemoveUserPayload(userDevice);
        sendEditMessage(payload);
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
