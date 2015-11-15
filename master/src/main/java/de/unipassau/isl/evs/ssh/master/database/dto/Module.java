package de.unipassau.isl.evs.ssh.master.database.dto;

import de.unipassau.isl.evs.ssh.core.util.DeviceID;

/**
 * A DTO representing modules which are connected to slave devices, e..g. a light.
 */
public class Module {

    private String name;
    private DeviceID atSlave;

}