package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.database.dto.Group;

/**
 * The SetGroupNamePayload is the payload used to set the name of a group.
 *
 * @author Wolfgang Popp.
 */
public class SetGroupNamePayload implements MessagePayload {
    private final Group group;
    private final String newName;

    /**
     * Constructs a new SetGroupNamePayload with the given group and name.
     *
     * @param group   the group to edit
     * @param newName the new name of the group
     */
    public SetGroupNamePayload(Group group, String newName) {
        this.group = group;
        this.newName = newName;
    }

    /**
     * Gets the group to edit.
     *
     * @return the group to edit
     */
    public Group getGroup() {
        return group;
    }

    /**
     * Gets the new name of the group.
     *
     * @return the new name
     */
    public String getNewName() {
        return newName;
    }
}
