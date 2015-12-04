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

    private List<UserInfoListener> listeners = new LinkedList<>();

    public interface UserInfoListener{
        void userInfoUpdated();
    }

    public void addUserInfoListener(AppUserConfigurationHandler.UserInfoListener listener) {
        listeners.add(listener);
    }

    public void removeUserInfoListener(AppUserConfigurationHandler.UserInfoListener listener) {
        listeners.remove(listener);
    }

    private void fireUserInfoUpdated(){
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
                fireUserInfoUpdated();
            }
        } else {
            throw new UnsupportedOperationException("Bad routing key.");
        }
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {

    }

    public List<Group> getAllGroups() {
        return Lists.newArrayList(groupToUserDevice.keySet());
    }

    public List<UserDevice> getAllUserDevices() {
        return Lists.newArrayList(usersToPermissions.keySet());
    }

    public List<UserDevice> getAllGroupMembers(Group group) {
        return groupToUserDevice.get(group);
    }

    public List<Permission> getAllPermissions() {
        return ImmutableList.copyOf(allPermissions);
    }

    public List<Permission> getPermissionForUser(UserDevice user) {
        return usersToPermissions.get(user);
    }

    //addGroup(Group group)
    //removeGroup(Group group)
    //editGroup(Group newGroup, Group oldGroup)
    //setPermission(UserDevice device, Permission permission)
    //editUserDevice(UserDevice newDevice, UserDevice oldDevice)
    //removeUserDevice(UserDevice userDevice)
}
