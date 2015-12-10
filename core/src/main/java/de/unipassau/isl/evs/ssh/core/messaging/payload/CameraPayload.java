package de.unipassau.isl.evs.ssh.core.messaging.payload;

import android.hardware.Camera;

/**
 * Payload class for messages regarding camera interaction
 *
 * @author Chris
 */
public class CameraPayload implements MessagePayload {

    String moduleName;
    int cameraID;
    byte[] picture;

    /**
     * Constructor for a CameraPayload only requesting the cameraID.
     * <p>
     * If this is used the picture in the payload will be null.
     * This can be used if a picture is requested.+
     *
     * @param cameraID of the camera which we interact with
     */
    public CameraPayload(int cameraID, String moduleName) {
        this.cameraID = cameraID;
        this.moduleName = moduleName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    /**
     * Returns the cameraID
     *
     * @return cameraID
     */
    public int getCameraID() {
        return cameraID;
    }

    public byte[] getPicture() {
        return picture;
    }

    public void setPicture(byte[] picture) {
        this.picture = picture;
    }
}
