package de.unipassau.isl.evs.ssh.core.database.dto;

import java.io.Serializable;

/**
 * A DTO representing a group. User devices are part of a group. Every group has a permission template.
 *
 * @author Leon Sell
 */
public class Group implements Serializable {
    private String name;
    private String templateName;

    public Group() {
    }

    public Group(String name, String templateName) {
        this.name = name;
        this.templateName = templateName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Group group = (Group) o;

        if (name != null ? !name.equals(group.name) : group.name != null) return false;
        return !(templateName != null ? !templateName.equals(group.templateName) : group.templateName != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (templateName != null ? templateName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return name;
    }
}