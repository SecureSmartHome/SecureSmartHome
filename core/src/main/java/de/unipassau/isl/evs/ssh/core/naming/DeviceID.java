package de.unipassau.isl.evs.ssh.core.naming;

/**
 * Unique id for all devices (user devices, master, slaves).
 */
public class DeviceID {

    private String id;

    public DeviceID(String id) {
        this.id = id;
    }


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

    @Override
    public String toString() {
        return "DeviceID{id='" + id + '\'' + '}';
    }
}