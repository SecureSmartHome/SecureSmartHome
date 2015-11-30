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
        this.id = id != null ? id.trim() : "";
    }

    /**
     * Returns the device id string.
     *
     * @return the device id
     * @deprecated use {@link #getIDString()} instead
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the device id string.
     *
     * @return the device id
     */
    public String getIDString() {
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
        return id.equals(deviceID.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public String toShortString() {
        if (id.isEmpty()) {
            return "???";
        } else {
            return id.substring(0, Math.min(id.length(), 7));
        }
    }

    @Override
    public String toString() {
        return getIDString();
    }
}
