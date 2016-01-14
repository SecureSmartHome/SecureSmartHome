package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.database.dto.Group;

/**
 * The SetGroupTemplatePayload is the payload used to set the template of a group.
 *
 * @author Wolfgang Popp.
 */
public class SetGroupTemplatePayload implements MessagePayload {
    private final Group group;
    private final String templateName;

    /**
     * Constructs a new SetGroupTemplatePayload withc the given group and template name.
     *
     * @param group        the group to edit
     * @param templateName the template to set for the given group
     */
    public SetGroupTemplatePayload(Group group, String templateName) {
        this.group = group;
        this.templateName = templateName;
    }

    /**
     * Gets the group to edit.
     *
     * @return the group to edit
     */
    public Group getGroup() {
        return group;
    }

    /**
     * Gets the new template that will be set for the group.
     *
     * @return the template name to set
     */
    public String getTemplateName() {
        return templateName;
    }
}
