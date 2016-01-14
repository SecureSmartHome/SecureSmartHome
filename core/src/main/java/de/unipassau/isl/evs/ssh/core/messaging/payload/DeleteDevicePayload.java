package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * @author Wolfgang Popp.
 */
public class DeleteDevicePayload implements MessagePayload {
    private final DeviceID user;

    public DeleteDevicePayload(DeviceID user) {
        this.user = user;
    }

    public DeviceID getUser() {
        return user;
    }
}
