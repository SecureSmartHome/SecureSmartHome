package de.unipassau.isl.evs.ssh.core.database.dto;

import java.io.Serializable;

/**
 * A DTO representing permissions. A permission has a name and may only be for a specific module.
 *
 * @author Leon Sell
 */
public class Permission implements Serializable {
    String name;
    String moduleName;

    public Permission() {
    }

    public Permission(String name) {
        this.name = name;
    }

    public Permission(String name, String moduleName) {
        this.name = name;
        this.moduleName = moduleName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }
}
