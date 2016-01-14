package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * @author Wolfgang Popp.
 */
public class SetUserGroupPayload implements MessagePayload {
    private final String groupName;
    private final DeviceID user;

    public SetUserGroupPayload(DeviceID user, String group) {
        this.groupName = group;
        this.user = user;
    }

    public String getGroupName() {
        return groupName;
    }

    public DeviceID getUser() {
        return user;
    }
}
