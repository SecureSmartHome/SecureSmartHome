package de.unipassau.isl.evs.ssh.core.messaging.payload;

import java.util.List;
import java.util.Map;

import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;

/**
 * The UserDevicePayload is used to transfer user, group and permission information.
 */
public class UserDevicePayload implements MessagePayload {

    private Map<UserDevice, List<Permission>> usersToPermissions;
    private Map<Group, List<UserDevice>> groupToUserDevice;
    private List<Permission> allPermissions;

    public UserDevicePayload(Map<UserDevice, List<Permission>> usersToPermissions, Map<Group, List<UserDevice>> groupToUserDevice, List<Permission> allPermissions) {
        this.usersToPermissions = usersToPermissions;
        this.groupToUserDevice = groupToUserDevice;
        this.allPermissions = allPermissions;
    }

    public Map<UserDevice, List<Permission>> getUsersToPermissions() {
        return usersToPermissions;
    }

    public Map<Group, List<UserDevice>> getGroupToUserDevice() {
        return groupToUserDevice;
    }

    public List<Permission> getAllPermissions() {
        return allPermissions;
    }
}
