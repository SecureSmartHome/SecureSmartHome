package de.unipassau.isl.evs.ssh.core.messaging.payload;

/**
 * Payload class for messages regarding door bell events
 *
 * @author Christoph Fraedrich
 */
public class DoorBellPayload implements MessagePayload {

    private final String moduleName; //Name of the bell which is rang
    private CameraPayload cameraPayload; //Picture of the door camera

    public DoorBellPayload(String moduleName) {
        this.moduleName = moduleName;
    }

    public CameraPayload getCameraPayload() {
        return cameraPayload;
    }

    public void setCameraPayload(CameraPayload cameraPayload) {
        this.cameraPayload = cameraPayload;
    }

    public String getModuleName() {
        return moduleName;
    }
}
