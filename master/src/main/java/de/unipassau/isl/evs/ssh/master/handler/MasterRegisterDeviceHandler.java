package de.unipassau.isl.evs.ssh.master.handler;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.master.database.DatabaseContract;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;

import java.util.List;
import java.util.Map;

/**
 * Handles messages indicating that a device wants to register itself at the system and also generates
 * messages for each target that needs to know of this event and passes them to the OutgoingRouter.
 */
public class MasterRegisterDeviceHandler extends AbstractMasterHandler{

    @Override
    public void handle(Message.AddressedMessage message) {
        final List<Group> groups = getContainer().require(UserManagementController.KEY).getGroups();
        final List<UserDevice> userDevices = getContainer().require(UserManagementController.KEY).getUserDevices();
        List<Permission> permissions = getContainer().require(PermissionController.KEY).getPermissions();

        ImmutableMap<Group, UserDevice> groupDeviceMapping = Maps.uniqueIndex(userDevices,
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

        ImmutableMap<UserDevice, Permission> devicePermissionMapping = Maps.uniqueIndex(permissions,
                new Function<Permission, UserDevice>() {
                    @Override
                    public UserDevice apply(Permission input) {
                        for (UserDevice userDevice : userDevices) {
                            if (getContainer().require(PermissionController.KEY).hasPermission(userDevice.getUserDeviceID(), input)) {
                                return userDevice;
                            }
                        }
                        return null;
                    }
                });
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {

    }
}