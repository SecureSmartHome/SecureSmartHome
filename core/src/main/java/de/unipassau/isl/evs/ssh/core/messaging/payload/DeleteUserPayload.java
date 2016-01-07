package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * @author Wolfgang Popp.
 */
public class DeleteUserPayload implements MessagePayload {
    private DeviceID user;

    public DeleteUserPayload(DeviceID user) {
        this.user = user;
    }

    public DeviceID getUser() {
        return user;
    }
}
