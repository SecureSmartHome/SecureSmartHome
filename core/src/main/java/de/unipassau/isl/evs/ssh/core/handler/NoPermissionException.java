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

package de.unipassau.isl.evs.ssh.core.handler;

import de.unipassau.isl.evs.ssh.core.sec.Permission;

/**
 * Thrown to indicate that the user does not have the necessary permission to execute an action.
 *
 * @author Wolfgang Popp.
 */
public class NoPermissionException extends Exception {
    private final Permission missingPermission;

    /**
     * Constructs a new MissingPermissionException indicating that the given permission is missing.
     *
     * @param missingPermission the permission that is missing to execute an action
     */
    public NoPermissionException(Permission missingPermission) {
        super("Missing PermissionDTO: " + missingPermission.toString());
        this.missingPermission = missingPermission;
    }

    /**
     * Gets the missing permission that was the cause of this exception to be thrown.
     *
     * @return the missing permission
     */
    public Permission getMissingPermission() {
        return missingPermission;
    }
}
