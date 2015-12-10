package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * @author Leon Sell
 */
public class RegisterSlavePayload implements MessagePayload {
    private String name;
    private DeviceID slaveID;

    public RegisterSlavePayload(Slave slave) {
        this.name = slave.getName();
        this.slaveID = slave.getSlaveID();
    }

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
