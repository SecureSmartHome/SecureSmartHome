package de.unipassau.isl.evs.ssh.master.database.dto;

import de.unipassau.isl.evs.ssh.core.util.DeviceID;

/**
 * A DTO representing modules which are connected to slave devices, e..g. a light.
 */
public class Module {

    private String name;
    private DeviceID atSlave;

    public Module() {
    }

    public Module(String name, DeviceID atSlave) {
        this.name = name;
        this.atSlave = atSlave;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DeviceID getAtSlave() {
        return atSlave;
    }

    public void setAtSlave(DeviceID atSlave) {
        this.atSlave = atSlave;
    }
}