package de.unipassau.isl.evs.ssh.core.database.dto;

import java.io.Serializable;

import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.ModuleAccessPoint;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * A DTO representing modules which are connected to slave devices, e..g. a light.
 * @author leon
 */
public class Module implements Serializable {

    private String name;
    private DeviceID atSlave;
    private String moduleType;
    private ModuleAccessPoint moduleAccessPoint;

    public Module() {
    }

    public Module(String name, DeviceID atSlave, String moduleType, ModuleAccessPoint moduleAccessPoint) {
        this.name = name;
        this.atSlave = atSlave;
        this.moduleType = moduleType;
        this.moduleAccessPoint = moduleAccessPoint;
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

    public String getModuleType() {
        return moduleType;
    }

    public void setModuleType(String moduleType) {
        this.moduleType = moduleType;
    }

    public ModuleAccessPoint getModuleAccessPoint() {
        return moduleAccessPoint;
    }

    public void setModuleAccessPoint(ModuleAccessPoint moduleAccessPoint) {
        this.moduleAccessPoint = moduleAccessPoint;
    }
}