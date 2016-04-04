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
 * The GroupPayload is the payload used to create or delete a group.
 *
 * @author Wolfgang Popp
 */
public class GroupPayload implements MessagePayload {
    /**
     * The action indicating whether to create or delete the group
     */
    public enum ACTION {
        CREATE, DELETE
    }

    private final Group group;
    private final ACTION action;

    /**
     * Constructs a new GroupPayload.
     *
     * @param group  the group to create or delete
     * @param action indicating whether to create or delete the given group
     */
    public GroupPayload(Group group, ACTION action) {
        this.group = group;
        this.action = action;
    }

    /**
     * The group to create or delete.
     *
     * @return the group
     */
    public Group getGroup() {
        return group;
    }

    /**
     * Gets the action of this payload.
     *
     * @return the action indicating whether to create or delete the group.
     */
    public ACTION getAction() {
        return action;
    }
}
