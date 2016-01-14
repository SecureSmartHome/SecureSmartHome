package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * The SetUserNamePayload is the payload used to set the name of a user.
 *
 * @author Wolfgang Popp.
 */
public class SetUserNamePayload implements MessagePayload {
    private final DeviceID user;
    private final String username;

    /**
     * Constructs a new SetUserNamePayload with the given user and username.
     *
     * @param user     the id of the user whose name is to be set
     * @param username the new name of the user
     */
    public SetUserNamePayload(DeviceID user, String username) {
        this.user = user;
        this.username = username;
    }

    /**
     * Gets the device id of the user that is edited.
     *
     * @return the device id of the user
     */
    public DeviceID getUser() {
        return user;
    }

    /**
     * Gets the new username.
     *
     * @return the new username
     */
    public String getUsername() {
        return username;
    }
}
