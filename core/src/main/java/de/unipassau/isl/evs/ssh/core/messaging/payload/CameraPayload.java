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
    PictureCallback pictureCallback;

    /**
     * Constructor for a CameraPayload only requesting the cameraID.
     *
     * If this is used the picture in the payload will be null.
     * This can be used if a picture is requested.+
     *
     * @param cameraID of the camera which we interact with
     */
    public CameraPayload(int cameraID, String moduleName) {
        this.cameraID = cameraID;
        this.moduleName = moduleName;
        this.pictureCallback = new PictureCallback();
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

    /**
     * Returns the taken picture
     *
     * @return picture taken
     */
    public PictureCallback getPictureCallback() {
        return pictureCallback;
    }

    /**
     * Allows setting the pictureCallback of this camera
     *
     * @param pictureCallback
     */
    public void setPictureCallback(PictureCallback pictureCallback) {
        this.pictureCallback = pictureCallback;
    }

    public class PictureCallback implements Camera.PictureCallback {
        byte[] pictureData;

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            this.pictureData = data;
        }
    }
}
