package de.unipassau.isl.evs.ssh.core.handler;

import de.unipassau.isl.evs.ssh.core.sec.Permission;

/**
 * Thrown to indicate that the user does not have the necessary permission to execute an action.
 *
 * @author Wolfgang Popp.
 */
public class NoPermissionException extends Exception {
    private final Permission missingPermission;

    /**
     * Constructs a new MissingPermissionException indicating that the given permission is missing.
     *
     * @param missingPermission the permission that is missing to execute an action
     */
    public NoPermissionException(Permission missingPermission) {
        super("Missing Permission: " + missingPermission.toString());
        this.missingPermission = missingPermission;
    }

    /**
     * Gets the missing permission that was the cause of this exception to be thrown.
     *
     * @return the missing permission
     */
    public Permission getMissingPermission() {
        return missingPermission;
    }
}
