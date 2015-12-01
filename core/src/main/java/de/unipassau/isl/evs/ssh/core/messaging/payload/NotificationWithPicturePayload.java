package de.unipassau.isl.evs.ssh.core.messaging.payload;

public class NotificationWithPicturePayload implements MessagePayload {
    private String type;
    private String message;
    private CameraPayload cameraPayload;

    public NotificationWithPicturePayload(String type, String message, CameraPayload cameraPayload) {
        this.type = type;
        this.message = message;
        this.cameraPayload = cameraPayload;
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

    public CameraPayload getCameraPayload() {
        return cameraPayload;
    }

    public void setCameraPayload(CameraPayload cameraPayload) {
        this.cameraPayload = cameraPayload;
    }
}
