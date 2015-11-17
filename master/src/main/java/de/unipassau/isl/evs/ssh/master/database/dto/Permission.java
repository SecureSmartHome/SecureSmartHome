package de.unipassau.isl.evs.ssh.master.database.dto;

/**
 * A DTO which represents a single permission of a single user.
 */
public class Permission {

    private String permissionName;
    private boolean hasPermission;

    public Permission() {
    }

    public Permission(String permissionName, boolean hasPermission) {
        this.permissionName = permissionName;
        this.hasPermission = hasPermission;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public boolean isHasPermission() {
        return hasPermission;
    }

    public void setHasPermission(boolean hasPermission) {
        this.hasPermission = hasPermission;
    }
}