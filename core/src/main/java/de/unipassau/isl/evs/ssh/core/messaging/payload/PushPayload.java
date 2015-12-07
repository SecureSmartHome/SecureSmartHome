package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * @author Chris
 */
public class PushPayload implements MessagePayload {

    DeviceID deviceID;

    public DeviceID getDeviceID() {
        return deviceID;
    }

    public PushPayload(DeviceID deviceID) {
        this.deviceID = deviceID;
    }
}
