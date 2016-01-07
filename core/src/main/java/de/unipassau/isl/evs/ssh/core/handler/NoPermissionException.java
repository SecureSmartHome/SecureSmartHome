package de.unipassau.isl.evs.ssh.core.handler;

import de.unipassau.isl.evs.ssh.core.sec.Permission;

/**
 * @author Wolfgang Popp.
 */
public class NoPermissionException extends Exception{
    private Permission missingPermission;

    public NoPermissionException(Permission missingPermission) {
        super("Missing Permission" + missingPermission.toString());
        this.missingPermission = missingPermission;
    }

    public Permission getMissingPermission() {
        return missingPermission;
    }
}
