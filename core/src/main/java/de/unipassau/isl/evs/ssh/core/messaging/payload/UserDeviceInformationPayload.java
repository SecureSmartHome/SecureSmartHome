package de.unipassau.isl.evs.ssh.core.messaging.payload;

import com.google.common.collect.ListMultimap;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;

/**
 * The UserDeviceInformationPayload is used to transfer user, group and permission information.
 *
 * @author Christoph Fraedrich
 */
public class UserDeviceInformationPayload implements MessagePayload {
    /**
     * If a device has no permissions, it is still in the map with an empty permission list
     */
    private ListMultimap<UserDevice, Permission> usersToPermissions;
    /**
     * If a group contains no devices, it is still in the map with an empty devicelist
     */
    private ListMultimap<Group, UserDevice> groupToUserDevice;
    private List<Permission> allPermissions;
    private List<Group> allGroups;
    private List<String> templates;

    public UserDeviceInformationPayload(ListMultimap<UserDevice, Permission> usersToPermissions,
                                        ListMultimap<Group, UserDevice> groupToUserDevice,
                                        List<Permission> allPermissions,
                                        List<Group> allGroups,
                                        List<String> templates
    ) {
        this.usersToPermissions = usersToPermissions;
        this.groupToUserDevice = groupToUserDevice;
        this.allPermissions = allPermissions;
        this.allGroups = allGroups;
        this.templates = templates;
    }

    public ListMultimap<UserDevice, Permission> getUsersToPermissions() {
        return usersToPermissions;
    }

    public ListMultimap<Group, UserDevice> getGroupToUserDevice() {
        return groupToUserDevice;
    }

    public List<Permission> getAllPermissions() {
        return allPermissions;
    }

    public List<Group> getAllGroups() {
        return allGroups;
    }

    public List<String> getTemplates() {
        return templates;
    }

    public void setTemplates(List<String> templates) {
        this.templates = templates;
    }
}
