/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
