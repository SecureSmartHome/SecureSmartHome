package de.unipassau.isl.evs.ssh.core.database.dto;

import java.io.Serializable;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * A DTO representing user device. A user device is a device using the SSH as a client.
 * User devices are part of a group.
 *
 * @author Leon Sell
 */
public class UserDevice implements Serializable, NamedDTO {
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

    @Override
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserDevice that = (UserDevice) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (inGroup != null ? !inGroup.equals(that.inGroup) : that.inGroup != null) return false;
        return !(userDeviceID != null ? !userDeviceID.equals(that.userDeviceID) : that.userDeviceID != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (inGroup != null ? inGroup.hashCode() : 0);
        result = 31 * result + (userDeviceID != null ? userDeviceID.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return name;
    }
}