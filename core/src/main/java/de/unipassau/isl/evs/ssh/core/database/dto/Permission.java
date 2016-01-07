package de.unipassau.isl.evs.ssh.core.database.dto;

import com.google.common.base.Strings;

import java.io.Serializable;

/**
 * A DTO representing permissions. A permission has a name and may only be for a specific module.
 * <p/>
 * //TODO resolve name collision with {@link de.unipassau.isl.evs.ssh.core.sec.Permission} (Niko, 2015-12-20)
 *
 * @author Leon Sell
 */
public class Permission implements Serializable {
    de.unipassau.isl.evs.ssh.core.sec.Permission permission;
    String moduleName;

    public Permission(de.unipassau.isl.evs.ssh.core.sec.Permission permission) {
        this.permission = permission;
    }

    public Permission(de.unipassau.isl.evs.ssh.core.sec.Permission permission, String moduleName) {
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
    public String toString() {
        return "Permission " + permission.toString() + (Strings.isNullOrEmpty(moduleName) ?
                "" : " (module " + moduleName + ")");
    }
}
