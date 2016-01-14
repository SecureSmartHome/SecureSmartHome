package de.unipassau.isl.evs.ssh.core.messaging.payload;

import com.google.common.collect.ListMultimap;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.PermissionDTO;
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
    private final ListMultimap<UserDevice, PermissionDTO> usersToPermissions;
    /**
     * If a group contains no devices, it is still in the map with an empty devicelist
     */
    private final ListMultimap<Group, UserDevice> groupToUserDevice;
    private final List<PermissionDTO> allPermissions;
    private final List<Group> allGroups;
    private List<String> templates;

    public UserDeviceInformationPayload(ListMultimap<UserDevice, PermissionDTO> usersToPermissions,
                                        ListMultimap<Group, UserDevice> groupToUserDevice,
                                        List<PermissionDTO> allPermissions,
                                        List<Group> allGroups,
                                        List<String> templates
    ) {
        this.usersToPermissions = usersToPermissions;
        this.groupToUserDevice = groupToUserDevice;
        this.allPermissions = allPermissions;
        this.allGroups = allGroups;
        this.templates = templates;
    }

    public ListMultimap<UserDevice, PermissionDTO> getUsersToPermissions() {
        return usersToPermissions;
    }

    public ListMultimap<Group, UserDevice> getGroupToUserDevice() {
        return groupToUserDevice;
    }

    public List<PermissionDTO> getAllPermissions() {
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
