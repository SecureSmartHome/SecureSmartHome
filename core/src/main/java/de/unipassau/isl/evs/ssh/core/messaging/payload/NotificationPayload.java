package de.unipassau.isl.evs.ssh.core.messaging.payload;

public class NotificationPayload implements MessagePayload {
    private String type;
    private String message;

    public NotificationPayload(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
