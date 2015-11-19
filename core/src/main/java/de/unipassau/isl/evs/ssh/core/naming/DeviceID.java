package de.unipassau.isl.evs.ssh.core.naming;

/**
 * Unique id for all devices (user devices, master, slaves).
 */
public class DeviceID {

    private String id;

    public DeviceID(String id){
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}