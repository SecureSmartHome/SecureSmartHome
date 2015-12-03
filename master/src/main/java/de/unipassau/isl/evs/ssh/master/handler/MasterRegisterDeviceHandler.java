package de.unipassau.isl.evs.ssh.master.handler;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.UserDevicePayload;
import de.unipassau.isl.evs.ssh.master.database.DatabaseContract;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;

import java.util.List;
import java.util.Map;

/**
 * Handles messages indicating that a device wants to register itself at the system and also generates
 * messages for each target that needs to know of this event and passes them to the OutgoingRouter.
 *
 * @author Chris
 */
public class MasterRegisterDeviceHandler extends AbstractMasterHandler{

    @Override
    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof UserDevicePayload) {
            UserDevicePayload payload = generatePayload();
            final Message messageToSend = new Message(message.getPayload());
            messageToSend.putHeader(Message.HEADER_REPLY_TO_KEY, message.getRoutingKey());
            sendMessage(message.getFromID(),CoreConstants.RoutingKeys.APP_USERINFO_GET,messageToSend);
        } else {
            sendErrorMessage(message); //wrong payload received
        }
    }

    private UserDevicePayload generatePayload() {
        final List<Group> groups = getContainer().require(UserManagementController.KEY).getGroups();
        final List<UserDevice> userDevices = getContainer().require(UserManagementController.KEY).getUserDevices();
        List<Permission> permissions = getContainer().require(PermissionController.KEY).getPermissions();

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

        ImmutableListMultimap<UserDevice, Permission> devicePermissionMapping = Multimaps.index(permissions,
                new Function<Permission, UserDevice>() {
                    @Override
                    public UserDevice apply(Permission input) {
                        for (UserDevice userDevice : userDevices) {
                            if (getContainer().require(PermissionController.KEY)
                                    .hasPermission(userDevice.getUserDeviceID(), input)) {
                                return userDevice;
                            }
                        }
                        return null;
                    }
                });


        UserDevicePayload payload = new UserDevicePayload(devicePermissionMapping,groupDeviceMapping, permissions);
        return payload;
    }
}