package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.database.dto.PermissionDTO;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * The SetPermissionPayload is the payload used to set the permission of a user.
 *
 * @author Wolfgang Popp.
 */
public class SetPermissionPayload implements MessagePayload {
    private final DeviceID user;
    private final PermissionDTO permission;
    private final Action action;

    /**
     * The Action enum describes whether to grant or revoke the permission.
     */
    public enum Action {
        GRANT, REVOKE
    }

    /**
     * Constructs a new SetPermissionPayload with the given user, permission or action.
     *
     * @param user       the user which will be edited
     * @param permission the permission to grant or revoke
     * @param action     either Action.GRANT or ACTION.REVOKE
     */
    public SetPermissionPayload(DeviceID user, PermissionDTO permission, Action action) {
        this.user = user;
        this.permission = permission;
        this.action = action;
    }

    /**
     * Gets the user that will be edited.
     *
     * @return the user
     */
    public DeviceID getUser() {
        return user;
    }

    /**
     * Gets the permission that will be granted or revoked.
     *
     * @return the permission to grant or revoke from the user
     */
    public PermissionDTO getPermission() {
        return permission;
    }

    /**
     * The Action enum describes whether to grant or revoke the permission.
     */
    public enum Action {
        GRANT, REVOKE
    }

    /**
     * Gets the action - either grant or revoke.
     *
     * @return grant or revoke
     */
    public Action getAction() {
        return action;
    }
}
