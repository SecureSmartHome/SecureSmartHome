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

import java.io.Serializable;

import de.unipassau.isl.evs.ssh.core.sec.Permission;

/**
 * Generic Notification Payload to send information to Apps indicating which Notificaiton
 * should be shown. May contain additional arguments which have to be evaluated by the app
 * depending on the notification type.
 *
 * @author Leon Sell
 */
public class NotificationPayload implements MessagePayload {
    private NotificationType type;
    private Serializable[] args;

    public NotificationPayload(NotificationType type, Serializable... args) {
        this.type = type;
        this.args = args;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public Serializable[] getArgs() {
        return args;
    }

    public void setArgs(Serializable[] args) {
        this.args = args;
    }

    /**
     * This class contains constants for different types of notifications.
     *
     * @author Christoph Frädrich
     */
    public enum NotificationType {
        UNKNOWN(null),
        WEATHER_SERVICE_FAILED(Permission.WEATHER_WARNING),
        WEATHER_WARNING(Permission.WEATHER_WARNING),
        BRIGHTNESS_WARNING(Permission.BRIGHTNESS_WARNING),
        HUMIDITY_WARNING(Permission.HUMIDITY_WARNING),
        SYSTEM_HEALTH_WARNING(Permission.SYSTEM_HEALTH_WARNING),
        HOLIDAY_MODE_SWITCHED_ON(Permission.HOLIDAY_MODE_SWITCHED_ON),
        HOLIDAY_MODE_SWITCHED_OFF(Permission.HOLIDAY_MODE_SWITCHED_OFF),
        BELL_RANG(Permission.BELL_RANG),
        DOOR_UNLATCHED(Permission.DOOR_UNLATCHED),
        DOOR_LOCKED(Permission.DOOR_LOCKED),
        DOOR_UNLOCKED(Permission.DOOR_UNLOCKED);

        private final Permission receivePermission;

        NotificationType(Permission receivePermission) {
            this.receivePermission = receivePermission;
        }

        public Permission getReceivePermission() {
            return receivePermission;
        }
    }
}
