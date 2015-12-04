package de.unipassau.isl.evs.ssh.core.messaging.payload;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.ModuleAccessPoint;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * Payload class for message to add new Sensor to the System.
 *
 * @author bucher
 */
public class AddNewSensorPayload {

    private Module module;
    private List<Module> moduleList;
    private String name;
    private DeviceID atSlave;
    private String moduleType;
    private ModuleAccessPoint moduleAccessPoint;

    public AddNewSensorPayload(Module module,List<Module> moduleList, String name, DeviceID atSlave, String moduleType, ModuleAccessPoint moduleAccessPoint){
        this.module = module;
        this.moduleList = moduleList;
        this.name = name;
        this.atSlave = atSlave;
        this.moduleType = moduleType;
        this.moduleAccessPoint = moduleAccessPoint;
    }

    /**
     * Returns the Module that should be added to the System.
     *
     * @return module to add
     */
    public Module getModule() {
        return module;
    }

    /**
     * Returns a List of the Modules before the new module has been added to is.
     *
     * @return moduleList of old modules
     */
    public List<Module> getModuleList() {
        return moduleList;
    }

    /**
     * Returns the Name of the new Sensor.
     *
     * @return name of Sensor
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the ID of the Sensor to identify at which Slave it is located.
     *
     * @return ID at Salve
     */
    public DeviceID getAtSlave() {
        return atSlave;
    }

    /**
     * Return the Type of the new Sensor.
     *
     * @return Sensor Type
     */
    public String getModuleType() {
        return moduleType;
    }

    /**
     * Returns the Module Access Point of the new Sensor.
     *
     * @return Access Point of Sensor
     */
    public ModuleAccessPoint getModuleAccessPoint() {
        return moduleAccessPoint;
    }
}
