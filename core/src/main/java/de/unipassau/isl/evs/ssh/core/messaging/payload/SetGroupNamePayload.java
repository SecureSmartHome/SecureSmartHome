package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.database.dto.Group;

/**
 * @author Wolfgang Popp.
 */
public class SetGroupNamePayload implements MessagePayload {
    private Group group;
    private String newName;

    public SetGroupNamePayload(Group group, String newName) {
        this.group = group;
        this.newName = newName;
    }

    public Group getGroup() {
        return group;
    }

    public String getNewName() {
        return newName;
    }
}
