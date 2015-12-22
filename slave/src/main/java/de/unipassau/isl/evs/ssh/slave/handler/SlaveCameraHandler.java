package de.unipassau.isl.evs.ssh.slave.handler;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_CAMERA_GET;

/**
 * Handles messages requesting pictures from the camera (via API calls) and generates messages,
 * containing the pictures, and sends these to the master.
 *
 * @author Christoph Fraedrich
 */
public class SlaveCameraHandler extends AbstractMessageHandler {
    private static final String TAG = SlaveCameraHandler.class.getSimpleName();
    private Handler handler;
    private Camera camera;

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, RoutingKey routingKey) {
        super.handlerAdded(dispatcher, routingKey);
        if (handler == null) {
            handler = new Handler(requireComponent(ContainerService.KEY_CONTEXT).getMainLooper());
        }
    }

    /**
     * Will perform actions based on the message given, e.g. permission/sanity checks.
     *
     * @param message Message to handle.
     */
    @Override
    public void handle(final Message.AddressedMessage message) {
        if (SLAVE_CAMERA_GET.matches(message)) {
            final CameraPayload payload = SLAVE_CAMERA_GET.getPayload(message);

            camera = Camera.open();
            camera.addCallbackBuffer(new byte[getImageSize(camera.getParameters())]);
            handler.post(new Runnable() {
                @Override
                @SuppressWarnings("deprecation")
                public void run() {
                    try {
                        sendErrorMessage(message);
                        SurfaceView view = new SurfaceView(requireComponent(ContainerService.KEY_CONTEXT));
                        camera.setPreviewDisplay(view.getHolder());

                        camera.setPreviewCallbackWithBuffer(new PictureCallback(
                                payload.getCameraID(),
                                payload.getModuleName(),
                                message.getSequenceNr(),
                                message.getHeader(Message.HEADER_REPLY_TO_KEY)
                        ));
                        camera.startPreview();
                    } catch (RuntimeException | IOException e) {
                        Log.i(TAG, "Failed to connect to cam.", e);
                        sendErrorMessage(message);
                    }
                }
            });
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (camera != null) {
                        camera.stopPreview();
                        camera.release();
                        camera = null;
                    }
                }
            }, TimeUnit.SECONDS.toMillis(5));
        } else {
            invalidMessage(message);
        }
    }

    private int getImageSize(Camera.Parameters params) {
        int size = ImageFormat.getBitsPerPixel(params.getPictureFormat()) / 8;
        List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
        size = (size > 0 ? previewSizes.get(0).width * previewSizes.get(0).height * size : previewSizes.get(0).width * previewSizes.get(0).height * 4);
        return size;
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

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            try {
                Rect rect = new Rect(0, 0, 300, 300);
                //mPreviewSize.width,
                //mPreviewSize.height);
                YuvImage image = new YuvImage(data, ImageFormat.NV16, 300, 300, //TODO what picture size do we use?
                        //mPreviewSize.width,
                        //mPreviewSize.height, ,
                        null);

                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                image.compressToJpeg(rect, 80, outStream);
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
            } finally {
                camera.stopPreview();
                camera.release();
                camera = null;
                SlaveCameraHandler.this.camera = null;
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