package de.unipassau.isl.evs.ssh.master.database.dto;

/**
 * A DTO which represents a single permission of a single user.
 */
public class Permission {

    private String permission;
    private int hasPermission;

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public int getHasPermission() {
        return hasPermission;
    }

    public void setHasPermission(int hasPermission) {
        this.hasPermission = hasPermission;
    }
}