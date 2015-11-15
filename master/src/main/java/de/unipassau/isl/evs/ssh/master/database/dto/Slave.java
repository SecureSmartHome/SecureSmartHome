package de.unipassau.isl.evs.ssh.master.database.dto;

import de.unipassau.isl.evs.ssh.core.util.DeviceID;

/**
 * A DTO representing slave devices. A slave device is a which can provide Modules to the master device.
 */
public class Slave {

    private String name;
    private DeviceID slaveID;

}