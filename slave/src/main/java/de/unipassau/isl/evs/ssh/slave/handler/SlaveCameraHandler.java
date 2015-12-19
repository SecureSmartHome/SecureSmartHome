package de.unipassau.isl.evs.ssh.slave.handler;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.SLAVE_CAMERA_GET;

/**
 * Handles messages requesting pictures from the camera (via API calls) and generates messages,
 * containing the pictures, and sends these to the master.
 *
 * @author Christoph Fraedrich
 */
public class SlaveCameraHandler extends AbstractMessageHandler {
    private static final String TAG = SlaveCameraHandler.class.getSimpleName();

    /**
     * Will perform actions based on the message given, e.g. permission/sanity checks.
     *
     * @param message Message to handle.
     */
    @Override
    public void handle(Message.AddressedMessage message) {
        if (SLAVE_CAMERA_GET.matches(message)) {
            CameraPayload payload = (CameraPayload) message.getPayload();

            try {
                Camera camera = Camera.open();
                camera.setOneShotPreviewCallback(new PictureCallback(
                        payload.getCameraID(),
                        payload.getModuleName(),
                        message.getSequenceNr(),
                        message.getHeader(Message.HEADER_REPLY_TO_KEY)
                ));
                camera.startPreview();
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to connect to cam.", e);
                sendErrorMessage(message);
            }
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{SLAVE_CAMERA_GET};
    }

    public class PictureCallback implements Camera.PreviewCallback, Camera.PictureCallback {
        private final int cameraID;
        private final String moduleName;
        private final int replyToSequenceNr;
        private final String replyToKey;

        public PictureCallback(int cameraID, String moduleName, int replyToSequenceNr, String replyToKey) {
            this.cameraID = cameraID;
            this.moduleName = moduleName;
            this.replyToSequenceNr = replyToSequenceNr;
            this.replyToKey = replyToKey;
        }

        public void onPreviewFrame(byte[] data, Camera camera) {
            Rect rect = new Rect(0, 0, 300, 300);
            //mPreviewSize.width,
            //mPreviewSize.height);
            YuvImage image = new YuvImage(data, ImageFormat.NV16, 300, 300, //TODO what picture size do we use?
                    //mPreviewSize.width,
                    //mPreviewSize.height, ,
                    null);

            try {
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                image.compressToJpeg(rect, 100, outStream);
                outStream.flush();
                outStream.close();
                byte[] jpegData = outStream.toByteArray();
/*
                    if (previewRotate != 0)
                        jpegData = rotatePicture(jpegData, mPreviewSize.width,
                                mPreviewSize.height, previewRotate);
                        */
                onPictureTaken(jpegData, camera);
            } catch (IOException e) {
                Log.wtf(TAG, e.getMessage());
            }
        }

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            CameraPayload payload = new CameraPayload(cameraID, moduleName);
            payload.setPicture(data);
            Message reply = new Message(payload);
            reply.putHeader(Message.HEADER_REFERENCES_ID, replyToSequenceNr);
            sendMessageToMaster(replyToKey, reply);
        }
    }
}