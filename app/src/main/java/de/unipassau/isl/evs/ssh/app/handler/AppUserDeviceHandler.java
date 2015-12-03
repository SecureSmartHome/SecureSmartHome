package de.unipassau.isl.evs.ssh.app.handler;

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
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * todo
 *
 * @author Wolfgang Popp
 */

public class AppUserDeviceHandler extends AbstractComponent implements MessageHandler {
    public static final Key<AppUserDeviceHandler> KEY = new Key<>(AppUserDeviceHandler.class);

    List<UserInfoListener> listeners = new LinkedList<>();

    public interface UserInfoListener{
        void userInfoUpdated();
    }

    public void addUserInfoListener(AppUserDeviceHandler.UserInfoListener listener) {
        listeners.add(listener);
    }

    public void removeUserInfoListener(AppUserDeviceHandler.UserInfoListener listener) {
        listeners.remove(listener);
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

    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {

    }

    public List<Group> getAllGroups() {
        // TODO
        return null;
    }

    public List<UserDevice> getAllUserDevices() {
        // TODO
        return null;
    }

    public List<UserDevice> getAllGroupMembers(Group group) {
        // TODO
        return null;
    }

    public List<Permission> getPermissionForUser(UserDevice user) {
        return null;
    }
}
