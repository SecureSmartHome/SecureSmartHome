/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
