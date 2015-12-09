package de.unipassau.isl.evs.ssh.app.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Lists;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.GroupEditPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.UserDeviceEditPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.UserDeviceInformationPayload;

/**
 * The AppUserConfigurationHandler
 *
 * @author Wolfgang Popp
 */
public class AppUserConfigurationHandler extends AbstractComponent implements MessageHandler {
    public static final Key<AppUserConfigurationHandler> KEY = new Key<>(AppUserConfigurationHandler.class);

    private ImmutableListMultimap<UserDevice, Permission> usersToPermissions;
    private ImmutableListMultimap<Group, UserDevice> groupToUserDevice;
    private List<Permission> allPermissions;
    private List<Group> allGroups;

    private List<UserInfoListener> listeners = new LinkedList<>();

    public interface UserInfoListener {
        void userInfoUpdated();
    }

    public void addUserInfoListener(AppUserConfigurationHandler.UserInfoListener listener) {
        listeners.add(listener);
    }

    public void removeUserInfoListener(AppUserConfigurationHandler.UserInfoListener listener) {
        listeners.remove(listener);
    }

    private void fireUserInfoUpdated() {
        for (UserInfoListener listener : listeners) {
            listener.userInfoUpdated();
        }
    }

    @Override
    public void init(Container container) {
        super.init(container);
        container.require(IncomingDispatcher.KEY).registerHandler(this, CoreConstants.RoutingKeys.APP_USERINFO_GET);
    }

    @Override
    public void destroy() {
        getComponent(IncomingDispatcher.KEY).unregisterHandler(this, CoreConstants.RoutingKeys.APP_USERINFO_GET);
        super.destroy();
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        String routingKey = message.getRoutingKey();
        if (routingKey.equals(CoreConstants.RoutingKeys.APP_USERINFO_GET)) {
            if (message.getPayload() instanceof UserDeviceInformationPayload) {
                UserDeviceInformationPayload payload = (UserDeviceInformationPayload) message.getPayload();
                this.usersToPermissions = payload.getUsersToPermissions();
                this.groupToUserDevice = payload.getGroupToUserDevice();
                this.allPermissions = payload.getAllPermissions();
                this.allGroups = payload.getAllGroups();
                fireUserInfoUpdated();
            }
        } else {
            throw new UnsupportedOperationException("Bad routing key.");
        }
    }

    public void update() {
        UserDeviceInformationPayload payload = new UserDeviceInformationPayload();
        OutgoingRouter router = getComponent(OutgoingRouter.KEY);

        Message message = new Message(payload);

        message.putHeader(Message.HEADER_REPLY_TO_KEY, CoreConstants.RoutingKeys.APP_USERINFO_GET);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_USERINFO_GET, message);
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {

    }

    public List<Group> getAllGroups() {
        if (allGroups == null) {
            return null;
        }
        return ImmutableList.copyOf(allGroups);
    }

    public List<UserDevice> getAllUserDevices() {
        if (usersToPermissions == null) {
            return null;
        }
        return Lists.newArrayList(usersToPermissions.keySet());
    }

    public List<UserDevice> getAllGroupMembers(Group group) {
        if (groupToUserDevice == null) {
            return null;
        }
        return groupToUserDevice.get(group);
    }

    public List<Permission> getAllPermissions() {
        if (allPermissions == null) {
            return null;
        }
        return ImmutableList.copyOf(allPermissions);
    }

    public List<Permission> getPermissionForUser(UserDevice user) {
        if (usersToPermissions == null) {
            return null;
        }
        return usersToPermissions.get(user);
    }

    private void sendEditMessage(MessagePayload payload) {
        Message message = new Message(payload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, CoreConstants.RoutingKeys.APP_USERINFO_GET);
        OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_USERINFO_SET, message);
    }

    public void addGroup(Group group) {
        GroupEditPayload payload = GroupEditPayload.newAddGroupPayload(group);
        sendEditMessage(payload);
    }

    public void initiateAddUser(UserDevice user) {
        //TODO
    }

    public void removeGroup(Group group) {
        GroupEditPayload payload = GroupEditPayload.newRemoveGroupPayload(group);
        sendEditMessage(payload);
    }

    public void editGroup(Group oldGroup, Group newGroup) {
        GroupEditPayload payload = GroupEditPayload.newEditGroupPayload(oldGroup, newGroup);
        sendEditMessage(payload);
    }

    public void grantPermission(UserDevice device, Permission permission) {
        UserDeviceEditPayload payload = UserDeviceEditPayload.newGrantPermissionPayload(device, permission);
        sendEditMessage(payload);
    }

    public void revokePermission(UserDevice device, Permission permission) {
        UserDeviceEditPayload payload = UserDeviceEditPayload.newRevokePermissionPayload(device, permission);
        sendEditMessage(payload);
    }

    public void editUserDevice(UserDevice oldDevice, UserDevice newDevice) {
        UserDeviceEditPayload payload = UserDeviceEditPayload.newEditUserPayload(oldDevice, newDevice);
        sendEditMessage(payload);
    }

    public void removeUserDevice(UserDevice userDevice) {
        UserDeviceEditPayload payload = UserDeviceEditPayload.newRemoveUserPayload(userDevice);
        sendEditMessage(payload);
    }
}
