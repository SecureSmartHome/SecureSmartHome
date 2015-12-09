package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * @author leon
 */
public class RegisterSlavePayload {
    private String name;
    private DeviceID slaveID;

    public RegisterSlavePayload(String name, DeviceID slaveID) {
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
