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

package de.unipassau.isl.evs.ssh.app.handler;

/**
 * The UserConfigurationEvent indicates that an user configuration action occurred. These action occur when properties
 * of a user change, e.g. the name of a user changed.
 *
 * @author Wolfgang Popp.
 */
public class UserConfigurationEvent {
    private final boolean wasSuccessful;
    private final EventType type;

    /**
     * Constructs a new UserConfigurationEvent for a pushed message. Pushed messages are messages that are not a reply
     * to a former request.
     */
    public UserConfigurationEvent() {
        this.wasSuccessful = true;
        this.type = EventType.PUSH;
    }

    /**
     * Constructs a new UserConfigurationEvent.
     *
     * @param type          the type of action that triggered this event
     * @param wasSuccessful true if the reply to a former request was no error
     */
    public UserConfigurationEvent(EventType type, boolean wasSuccessful) {
        this.type = type;
        this.wasSuccessful = wasSuccessful;
    }

    /**
     * Gets the type of this event.
     *
     * @return the type of this event
     */
    public EventType getType() {
        return type;
    }

    /**
     * Checks weather the action corresponding to this event was successful.
     *
     * @return true if the action was successful
     */
    public boolean wasSuccessful() {
        return wasSuccessful;
    }

    /**
     * The EventType enum lists possible event types that occur when changing the user configuration.
     */
    public enum EventType {
        PUSH,
        PERMISSION_GRANT,
        PERMISSION_REVOKE,
        USERNAME_SET,
        USER_SET_GROUP,
        USER_DELETE,
        GROUP_ADD,
        GROUP_DELETE,
        GROUP_SET_NAME,
        GROUP_SET_TEMPLATE
    }
}
