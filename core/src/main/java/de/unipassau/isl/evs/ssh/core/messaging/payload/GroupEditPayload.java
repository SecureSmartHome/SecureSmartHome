package de.unipassau.isl.evs.ssh.core.messaging.payload;

import java.util.HashMap;
import java.util.Map;

import de.unipassau.isl.evs.ssh.core.database.dto.Group;

public class GroupEditPayload implements MessagePayload {

    private Group group;
    private Group oldGroup;
    private Group newGroup;
    private Action action;

    public enum Action {
        ADD_GROUP, REMOVE_GROUP, EDIT_GROUP,
    }

    private GroupEditPayload() {

    }

    public static GroupEditPayload newAddGroupPayload(Group group) {
        GroupEditPayload payload = new GroupEditPayload();
        payload.action = Action.ADD_GROUP;
        payload.group = group;
        return payload;
    }

    public static GroupEditPayload newRemoveGroupPayload(Group group) {
        GroupEditPayload payload = new GroupEditPayload();
        payload.action = Action.REMOVE_GROUP;
        payload.group = group;
        return payload;
    }

    public static GroupEditPayload newEditGroupPayload(Group oldGroup, Group newGroup) {
        GroupEditPayload payload = new GroupEditPayload();
        payload.action = Action.EDIT_GROUP;
        payload.oldGroup = oldGroup;
        payload.newGroup = newGroup;
        return payload;
    }

    public Action getAction() {
        return action;
    }

    public Map<Group, Group> getGroupToEdit() {
        if (action != Action.EDIT_GROUP) {
            throw new IllegalStateException("Invalid action");
        }
        Map<Group, Group> map = new HashMap<>(1);
        map.put(oldGroup, newGroup);
        return map;
    }

    public Group getGroupToAdd() {
        if (action != Action.ADD_GROUP) {
            throw new IllegalStateException("Invalid action");
        }
        return group;
    }

    public Group getGroupToRemove() {
        if (action != Action.ADD_GROUP) {
            throw new IllegalStateException("Invalid action");
        }
        return group;
    }
}
