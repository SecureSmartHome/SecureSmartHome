package de.unipassau.isl.evs.ssh.core.database.dto;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.ModuleAccessPoint;

/**
 * A DTO representing modules which are connected to slave devices, e..g. a light.
 * @author leon
 */
public class Module {

    private String name;
    private DeviceID atSlave;
    private ModuleAccessPoint moduleAccessPoint;

    public Module() {
    }

    public Module(String name, DeviceID atSlave, ModuleAccessPoint moduleAccessPoint) {
        this.name = name;
        this.atSlave = atSlave;
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

    public ModuleAccessPoint getModuleAccessPoint() {
        return moduleAccessPoint;
    }

    public void setModuleAccessPoint(ModuleAccessPoint moduleAccessPoint) {
        this.moduleAccessPoint = moduleAccessPoint;
    }
}