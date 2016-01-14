package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * The SetUserGroupPayload is the payload used to set the group of a user.
 *
 * @author Wolfgang Popp.
 */
public class SetUserGroupPayload implements MessagePayload {
    private final String groupName;
    private final DeviceID user;

    /**
     * Constructs a new SetUserGroupPayload with the given user and group.
     *
     * @param user  the user whose group is set
     * @param group the group to set
     */
    public SetUserGroupPayload(DeviceID user, String group) {
        this.groupName = group;
        this.user = user;
    }

    /**
     * Gets the group name that will be set for the user.
     *
     * @return the group name
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Gets the device id of the user whose group is to be set.
     *
     * @return the user to edit
     */
    public DeviceID getUser() {
        return user;
    }
}
