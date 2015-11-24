package de.unipassau.isl.evs.ssh.master.database.dto;

/**
 * A DTO representing permissions. A permission has a name and may only be for a specific module.
 * @author leon
 */
public class Permission {
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
