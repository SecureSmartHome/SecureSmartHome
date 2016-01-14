package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.database.dto.Group;

/**
 * @author Wolfgang Popp.
 */
public class SetGroupTemplatePayload implements MessagePayload {
    private final Group group;
    private final String templateName;

    public SetGroupTemplatePayload(Group group, String templateName) {
        this.group = group;
        this.templateName = templateName;
    }

    public Group getGroup() {
        return group;
    }

    public String getTemplateName() {
        return templateName;
    }
}
