package de.unipassau.isl.evs.ssh.core.messaging.payload;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableListMultimap;

import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;

/**
 * The UserDeviceInformationPayload is used to transfer user, group and permission information.
 *
 * @author Chris
 */
public class UserDeviceInformationPayload implements MessagePayload {

    private ImmutableListMultimap<UserDevice, Permission> usersToPermissions; //If a device has no permissions, it is still in the map with an empty permission list
    private ImmutableListMultimap<Group, UserDevice> groupToUserDevice; //If a group contains no devices, it is still in the map with an empty devicelist
    private List<Permission> allPermissions;

    public UserDeviceInformationPayload() {
        this.usersToPermissions = null;
        this.groupToUserDevice = null;
        this.allPermissions = null;
    }

    public UserDeviceInformationPayload(ImmutableListMultimap<UserDevice, Permission> usersToPermissions,
                                        ImmutableListMultimap<Group, UserDevice> groupToUserDevice, List<Permission> allPermissions) {
        this.usersToPermissions = usersToPermissions;
        this.groupToUserDevice = groupToUserDevice;
        this.allPermissions = allPermissions;
    }

    public ImmutableListMultimap<UserDevice, Permission> getUsersToPermissions() {
        return usersToPermissions;
    }

    public ImmutableListMultimap<Group, UserDevice> getGroupToUserDevice() {
        return groupToUserDevice;
    }

    public List<Permission> getAllPermissions() {
        return allPermissions;
    }
}
