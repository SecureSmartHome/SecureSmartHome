package de.unipassau.isl.evs.ssh.app.handler;

import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * todo
 *
 * @author Wolfgang Popp
 */

public class AppUserDeviceHandler extends AbstractComponent implements MessageHandler {
    public static final Key<AppUserDeviceHandler> KEY = new Key<>(AppUserDeviceHandler.class);

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
}
