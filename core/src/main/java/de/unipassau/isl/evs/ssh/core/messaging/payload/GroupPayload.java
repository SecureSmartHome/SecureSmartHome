package de.unipassau.isl.evs.ssh.core.messaging.payload;

import java.util.HashMap;
import java.util.Map;

import de.unipassau.isl.evs.ssh.core.database.dto.Group;

/**
 * @author Wolfgang Popp
 */
public class GroupPayload implements MessagePayload {
    public enum ACTION {
        CREATE, DELETE
    }

    private Group group;
    private ACTION action;

    public GroupPayload(Group group, ACTION action) {
        this.group = group;
        this.action = action;
    }

    public Group getGroup() {
        return group;
    }

    public ACTION getAction() {
        return action;
    }
}