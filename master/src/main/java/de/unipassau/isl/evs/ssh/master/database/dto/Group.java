package de.unipassau.isl.evs.ssh.master.database.dto;

/**
 * A DTO representing a group. User devices are part of a group. Every group has a permission template.
 * @author leon
 */
public class Group {

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
}