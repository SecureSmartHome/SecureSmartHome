package de.unipassau.isl.evs.ssh.master.network;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys;
import de.unipassau.isl.evs.ssh.core.messaging.payload.UserDeviceInformationPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;

/**
 * @author Wolfgang Popp.
 */
public class UserConfigurationBroadcaster extends AbstractComponent {
    public static final Key<UserConfigurationBroadcaster> KEY = new Key<>(UserConfigurationBroadcaster.class);

    public void updateAllClients() {
        final Iterable<DeviceID> connectedClients = requireComponent(Server.KEY).getActiveDevices();
        for (DeviceID connectedClient : connectedClients) {
            updateClient(connectedClient);
        }
    }

    public void updateClient(DeviceID id) {
        final Message message = new Message(generateUserDeviceInformationPayload());
        requireComponent(OutgoingRouter.KEY).sendMessage(id, RoutingKeys.APP_USERINFO_UPDATE, message);
    }

    private UserDeviceInformationPayload generateUserDeviceInformationPayload() {
        final PermissionController permissionController = requireComponent(PermissionController.KEY);
        final List<Group> groups;
        final List<UserDevice> userDevices;
        final List<String> templates;
        final List<Permission> permissions;
        groups = requireComponent(UserManagementController.KEY).getGroups();
        userDevices = requireComponent(UserManagementController.KEY).getUserDevices();
        permissions = requireComponent(PermissionController.KEY).getPermissions();
        templates = requireComponent(PermissionController.KEY).getTemplates();

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

        ListMultimap<UserDevice, Permission> userHasPermissions = ArrayListMultimap.create();
        for (UserDevice userDevice : userDevices) {
            userHasPermissions.putAll(userDevice, permissionController.getPermissionsOfUserDevice(userDevice.getUserDeviceID()));
        }

        return new UserDeviceInformationPayload(
                userHasPermissions,
                groupDeviceMapping,
                permissions,
                groups,
                templates
        );
    }
}
