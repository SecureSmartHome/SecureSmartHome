package de.unipassau.isl.evs.ssh.master.database.dto;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * A DTO representing user device. A user device is a device using the SSH as a client.
 * User devices are part of a group.
 * @author leon
 */
public class UserDevice {

    private String name;
    private String inGroup;
    private DeviceID userDeviceID;

    public UserDevice() {
    }

    public UserDevice(String name, String inGroup, DeviceID userDeviceID) {
        this.name = name;
        this.inGroup = inGroup;
        this.userDeviceID = userDeviceID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInGroup() {
        return inGroup;
    }

    public void setInGroup(String inGroup) {
        this.inGroup = inGroup;
    }

    public DeviceID getUserDeviceID() {
        return userDeviceID;
    }

    public void setUserDeviceID(DeviceID userDeviceID) {
        this.userDeviceID = userDeviceID;
    }
}