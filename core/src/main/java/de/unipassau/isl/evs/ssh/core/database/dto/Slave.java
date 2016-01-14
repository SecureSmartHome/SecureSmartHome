package de.unipassau.isl.evs.ssh.core.database.dto;

import java.io.Serializable;
import java.util.Arrays;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * A DTO representing slave devices. A slave device is a which can provide Modules to the master device.
 *
 * @author Leon Sell
 */
public class Slave implements Serializable, NamedDTO {
    private String name;
    private DeviceID slaveID;
    private byte[] passiveRegistrationToken;

    public Slave() {}

    public Slave(String name, DeviceID slaveID, byte[] passiveRegistrationToken) {
        this.name = name;
        this.slaveID = slaveID;
        this.passiveRegistrationToken = passiveRegistrationToken;
    }

    @Override
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

    public void setPassiveRegistrationToken(byte[] passiveRegistrationToken) {
        this.passiveRegistrationToken = passiveRegistrationToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Slave slave = (Slave) o;

        if (name != null ? !name.equals(slave.name) : slave.name != null) return false;
        if (slaveID != null ? !slaveID.equals(slave.slaveID) : slave.slaveID != null) return false;
        return Arrays.equals(passiveRegistrationToken, slave.passiveRegistrationToken);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (slaveID != null ? slaveID.hashCode() : 0);
        result = 31 * result + (passiveRegistrationToken != null ? Arrays.hashCode(passiveRegistrationToken) : 0);
        return result;
    }

    @Override
    public String toString() {
        return name;
    }
}