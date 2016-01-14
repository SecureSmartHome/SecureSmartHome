package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * The DeleteDevicePayload is the payload used to delete a UserDevice.
 *
 * @author Wolfgang Popp
 */
public class DeleteDevicePayload implements MessagePayload {
    private final DeviceID user;

    /**
     * Constructs a new DeleteDevicePayload that is used to delete the given user.
     *
     * @param user the device id of the user to delete
     */
    public DeleteDevicePayload(DeviceID user) {
        this.user = user;
    }

    /**
     * Gets the user that will be deleted.
     *
     * @return the user to delete
     */
    public DeviceID getUser() {
        return user;
    }
}
