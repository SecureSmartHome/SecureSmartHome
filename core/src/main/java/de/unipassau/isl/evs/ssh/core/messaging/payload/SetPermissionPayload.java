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

import de.unipassau.isl.evs.ssh.core.database.dto.PermissionDTO;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * The SetPermissionPayload is the payload used to set the permission of a user.
 *
 * @author Wolfgang Popp.
 */
public class SetPermissionPayload implements MessagePayload {
    private final DeviceID user;
    private final PermissionDTO permission;
    private final Action action;

    /**
     * The Action enum describes whether to grant or revoke the permission.
     */
    public enum Action {
        GRANT, REVOKE
    }

    /**
     * Constructs a new SetPermissionPayload with the given user, permission or action.
     *
     * @param user       the user which will be edited
     * @param permission the permission to grant or revoke
     * @param action     either Action.GRANT or ACTION.REVOKE
     */
    public SetPermissionPayload(DeviceID user, PermissionDTO permission, Action action) {
        this.user = user;
        this.permission = permission;
        this.action = action;
    }

    /**
     * Gets the user that will be edited.
     *
     * @return the user
     */
    public DeviceID getUser() {
        return user;
    }

    /**
     * Gets the permission that will be granted or revoked.
     *
     * @return the permission to grant or revoke from the user
     */
    public PermissionDTO getPermission() {
        return permission;
    }

    /**
     * Gets the action - either grant or revoke.
     *
     * @return grant or revoke
     */
    public Action getAction() {
        return action;
    }
}
