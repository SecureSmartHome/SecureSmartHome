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