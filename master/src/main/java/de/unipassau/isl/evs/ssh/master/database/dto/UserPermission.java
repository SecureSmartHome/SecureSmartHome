package de.unipassau.isl.evs.ssh.master.database.dto;

import de.unipassau.isl.evs.ssh.core.util.DeviceID;

public class UserPermission {

    private DeviceID userDeviceID;
    private Permission permission;

    public UserPermission() {
    }

    public UserPermission(DeviceID userDeviceID, Permission permission) {
        this.userDeviceID = userDeviceID;
        this.permission = permission;
    }

    public DeviceID getUserDeviceID() {
        return userDeviceID;
    }

    public void setUserDeviceID(DeviceID userDeviceID) {
        this.userDeviceID = userDeviceID;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }
}