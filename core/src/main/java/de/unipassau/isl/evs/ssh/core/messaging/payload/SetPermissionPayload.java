package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * @author Wolfgang Popp.
 */
public class SetPermissionPayload implements MessagePayload {
    private DeviceID user;
    private Permission permission;
    private Action action;

    public enum Action {
        GRANT, REVOKE
    }

    public SetPermissionPayload(DeviceID user, Permission permission, Action action) {
        this.user = user;
        this.permission = permission;
        this.action = action;
    }

    public DeviceID getUser() {
        return user;
    }

    public Permission getPermission() {
        return permission;
    }

    public Action getAction() {
        return action;
    }
}
