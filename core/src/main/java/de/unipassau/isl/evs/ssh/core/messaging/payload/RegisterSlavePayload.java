package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * @author Leon Sell
 */
public class RegisterSlavePayload implements MessagePayload {
    private String name;
    private DeviceID slaveID;
    private final byte[] passiveRegistrationToken;

    public RegisterSlavePayload(String name, DeviceID slaveID, byte[] passiveRegistrationToken) {
        this.name = name;
        this.slaveID = slaveID;
        this.passiveRegistrationToken = passiveRegistrationToken;
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

    public byte[] getPassiveRegistrationToken() {
        return passiveRegistrationToken;
    }
}
