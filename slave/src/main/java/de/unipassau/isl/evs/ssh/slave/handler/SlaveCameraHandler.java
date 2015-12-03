package de.unipassau.isl.evs.ssh.slave.handler;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;

/**
 * Handles messages requesting pictures from the camera (via API calls) and generates messages,
 * containing the pictures, and sends these to the master.
 *
 * @author Chris
 */
public class SlaveCameraHandler implements MessageHandler {


    private IncomingDispatcher dispatcher;

    /**
     * Will perform actions based on the message given, e.g. permission/sanity checks.
     *
     * @param message Message to handle.
     */
    @Override
    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof CameraPayload) {
            CameraPayload payload = (CameraPayload) message.getPayload();

            Camera camera;
            camera = Camera.open(payload.getCameraID());

            CameraPayload.PictureCallback pictureCallback = payload.getPictureCallback();
            setPreviewCallback(pictureCallback, camera);
            payload.setPictureCallback(pictureCallback);
        } else {
            sendErrorMessage(message);
        }
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void handlerRemoved(String routingKey) {

    }

    /**
     * Sends an error message to original sender
     *
     * @param original message
     */
    private void sendErrorMessage(Message.AddressedMessage original) {
        Message reply;

        String routingKey = original.getHeader(Message.HEADER_REPLY_TO_KEY);
        reply = new Message(new MessageErrorPayload(original.getPayload()));
        reply.putHeader(Message.HEADER_REFERENCES_ID, original.getSequenceNr());
        reply.putHeader(Message.HEADER_TIMESTAMP, System.currentTimeMillis());

        dispatcher.getContainer().require(OutgoingRouter.KEY).sendMessage(original.getFromID(), routingKey, reply);
    }

    /**
     * Makes a preview and takes a frame from there to make fill a picture callback
     *
     * @param pictureCallback to be filled
     * @param camera          to be used
     */
    private void setPreviewCallback(final Camera.PictureCallback pictureCallback, Camera camera) {
        Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
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
                    if (pictureCallback != null) {
                        pictureCallback.onPictureTaken(jpegData, camera);
                    }
                } catch (IOException e) {
                    Log.wtf("facerecognition", e.getMessage());
                }
            }

        };

        camera.setOneShotPreviewCallback(previewCallback);
        camera.startPreview();
    }
}