package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * @author Wolfgang Popp.
 */
public class SetUserNamePayload implements MessagePayload {
    private DeviceID user;
    private String username;

    public SetUserNamePayload(DeviceID user, String username) {
        this.user = user;
        this.username = username;
    }

    public DeviceID getUser() {
        return user;
    }

    public String getUsername() {
        return username;
    }
}
