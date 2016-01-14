package de.unipassau.isl.evs.ssh.core.database.dto;

import com.google.common.base.Strings;

import java.io.Serializable;

/**
 * A DTO representing permissions. A permission has a name and may only be for a specific module.
 *
 * @author Leon Sell
 */
public class PermissionDTO implements Serializable {
    private de.unipassau.isl.evs.ssh.core.sec.Permission permission;
    private String moduleName;

    public PermissionDTO(de.unipassau.isl.evs.ssh.core.sec.Permission permission) {
        this.permission = permission;
    }

    public PermissionDTO(de.unipassau.isl.evs.ssh.core.sec.Permission permission, String moduleName) {
        this.permission = permission;
        this.moduleName = moduleName;
    }

    public de.unipassau.isl.evs.ssh.core.sec.Permission getPermission() {
        return permission;
    }

    public void setPermission(de.unipassau.isl.evs.ssh.core.sec.Permission permission) {
        this.permission = permission;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PermissionDTO that = (PermissionDTO) o;

        if (permission != that.permission) return false;
        return !(moduleName != null ? !moduleName.equals(that.moduleName) : that.moduleName != null);

    }

    @Override
    public int hashCode() {
        int result = permission != null ? permission.hashCode() : 0;
        result = 31 * result + (moduleName != null ? moduleName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PermissionDTO " + permission.toString() + (Strings.isNullOrEmpty(moduleName) ?
                "" : " (module " + moduleName + ")");
    }
}
