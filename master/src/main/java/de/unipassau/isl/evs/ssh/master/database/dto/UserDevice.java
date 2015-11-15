package de.unipassau.isl.evs.ssh.master.database.dto;

import de.unipassau.isl.evs.ssh.core.util.DeviceID;

/**
 * A DTO representing user device. A user device is a device using the SSH as a client.
 * User devices are part of a group.
 */
public class UserDevice {

    private String name;
    private String inGroup;
    private DeviceID userDeviceID;

}