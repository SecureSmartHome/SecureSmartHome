package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.database.dto.Group;

/**
 * The GroupPayload is the payload used to create or delete a group.
 *
 * @author Wolfgang Popp
 */
public class GroupPayload implements MessagePayload {
    /**
     * The action indicating whether to create or delete the group
     */
    public enum ACTION {
        CREATE, DELETE
    }

    private final Group group;
    private final ACTION action;

    /**
     * Constructs a new GroupPayload.
     *
     * @param group  the group to create or delete
     * @param action indicating whether to create or delete the given group
     */
    public GroupPayload(Group group, ACTION action) {
        this.group = group;
        this.action = action;
    }

    /**
     * The group to create or delete.
     *
     * @return the group
     */
    public Group getGroup() {
        return group;
    }

    /**
     * Gets the action of this payload.
     *
     * @return the action indicating whether to create or delete the group.
     */
    public ACTION getAction() {
        return action;
    }
}
