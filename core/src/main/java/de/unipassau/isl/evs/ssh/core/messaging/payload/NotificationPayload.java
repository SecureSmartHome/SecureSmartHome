package de.unipassau.isl.evs.ssh.core.messaging.payload;

import java.io.Serializable;

import de.unipassau.isl.evs.ssh.core.sec.Permission;

/**
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
        //TODO use when Notification System gets changed
        UNKNOWN(null),
        WEATHER_WARNING(Permission.WEATHER_WARNING),
        HUMIDITY_WARNING(Permission.HUMIDITY_WARNING),
        BRIGHTNESS_WARNING(Permission.BRIGHTNESS_WARNING);

        private final Permission receivePermission;

        NotificationType(Permission receivePermission) {
            this.receivePermission = receivePermission;
        }

        public Permission getReceivePermission() {
            return receivePermission;
        }
    }
}
