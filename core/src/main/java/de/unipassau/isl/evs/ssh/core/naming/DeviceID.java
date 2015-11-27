package de.unipassau.isl.evs.ssh.core.naming;

import java.io.Serializable;

/**
 * Unique id for all devices (user devices, master, slaves).
 *
 * @author Wolfgang Popp
 */
public final class DeviceID implements Serializable {
    private final String id;

    /**
     * Creates a new DeviceID from the given string.
     *
     * @param id the id as string
     */
    public DeviceID(String id) {
        this.id = id != null ? id.trim() : id;
    }

    /**
     * Returns the device id string.
     *
     * @return the device id
     */
    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DeviceID deviceID = (DeviceID) o;
        if (id != null) {
            return id.equals(deviceID.id);
        } else {
            return deviceID.id == null;
        }
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public String toShortString() {
        if (id == null) {
            return "???";
        } else {
            return id.substring(0, Math.min(id.length(), 7));
        }
    }

    @Override
    public String toString() {
        return '\'' + id + '\'';
    }
}
