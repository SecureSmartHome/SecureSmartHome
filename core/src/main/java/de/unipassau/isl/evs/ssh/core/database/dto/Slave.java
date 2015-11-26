package de.unipassau.isl.evs.ssh.core.database.dto;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * A DTO representing slave devices. A slave device is a which can provide Modules to the master device.
 * @author leon
 */
public class Slave {

    private String name;
    private DeviceID slaveID;

    public Slave() {
    }

    public Slave(String name, DeviceID slaveID) {
        this.name = name;
        this.slaveID = slaveID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DeviceID getSlaveID() {
        return slaveID;
    }

    public void setSlaveID(DeviceID slaveID) {
        this.slaveID = slaveID;
    }
}