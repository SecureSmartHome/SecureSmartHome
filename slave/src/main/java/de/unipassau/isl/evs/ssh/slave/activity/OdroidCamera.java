package de.unipassau.isl.evs.ssh.slave.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.slave.R;
import de.unipassau.isl.evs.ssh.slave.SlaveContainer;

/**
 * @author Tobias Marktscheffel
 * @author Christoph Fraedrich
 * @author Niko Fink
 */
@SuppressWarnings("deprecation")
public class OdroidCamera extends BoundActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private static final String TAG = OdroidCamera.class.getSimpleName();
    private static final String EXTRA_CAMERA_ID = OdroidCamera.class.getName() + ".CameraID";
    private static final String EXTRA_MODULE_NAME = OdroidCamera.class.getName() + ".ModuleName";
    private static final String EXTRA_REPLY_TO_SEQUENCE_NR = OdroidCamera.class.getName() + ".ReplyToSequenceNr";
    private static final String EXTRA_REPLY_TO_KEY = OdroidCamera.class.getName() + ".ReplyToKey";

    private Camera camera;
    private Camera.Parameters params;

    private byte[] lastSnapshot;

    public OdroidCamera() {
        super(SlaveContainer.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");

        //setVisible(false);
        setContentView(R.layout.activity_camera);
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "surfaceCreated(holder = [" + holder + "])");
        camera = Camera.open();
        params = camera.getParameters();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged(holder = [" + holder + "], format = [" + format + "], width = [" + width + "], height = [" + height + "])");
        if (holder.getSurface() == null) return;
        try {
            camera.setPreviewDisplay(holder);
            camera.addCallbackBuffer(new byte[getImageSize()]);
            camera.setPreviewCallbackWithBuffer(this);
            camera.startPreview();
        } catch (IOException ioe) {
            Log.e(TAG, "Could not set preview display", ioe);
        }
    }

    private int getImageSize() {
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        for (Camera.Size size : sizes) {
            Log.d(TAG, "Supported Preview Size: " + size.width + "x" + size.height);
        }
        int size = ImageFormat.getBitsPerPixel(params.getPictureFormat()) / 8;
        size = (size > 0 ? sizes.get(0).width * sizes.get(0).height * size : sizes.get(0).width * sizes.get(0).height * 4);
        return size;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "surfaceDestroyed(holder = [" + holder + "])");
        if (camera == null) return;
        camera.stopPreview();
        camera.release();
    }

    @Override
    public void onPreviewFrame(byte[] raw, Camera cam) {
        Log.d(TAG, "onPreviewFrame(raw = byte[" + raw.length + "], cam = [" + cam + "])");
        lastSnapshot = raw;
        maybeSendImage();
    }

    @Override
    public void onContainerConnected(Container container) {
        maybeSendImage();
    }

    private void maybeSendImage() {
        if (getContainer() == null || lastSnapshot == null) {
            return;
        }
        int width = params.getPreviewSize().width;
        int height = params.getPreviewSize().height;
        Rect rect = new Rect(0, 0, width, height);
        YuvImage yuvimage = new YuvImage(lastSnapshot, ImageFormat.NV21, width, height, null);

        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            yuvimage.compressToJpeg(rect, 80, outStream);
            byte[] jpegData = outStream.toByteArray();
            sendCompressedImage(jpegData);

            //File file = new File(Environment.getExternalStorageDirectory().getPath(),
            //        "snapshot" + System.currentTimeMillis() + ".jpg");
            //FileOutputStream outstr = new FileOutputStream(file);
            //yuvimage.compressToJpeg(rect, 80, outstr);
        } catch (IOException e) {
            Log.e(TAG, "Could not compress image", e);
        }

        finish();
    }

    private void sendCompressedImage(byte[] jpegData) {
        CameraPayload payload = new CameraPayload(getCameraID(), getModuleName());
        payload.setPicture(jpegData);
        Message reply = new Message(payload);
        reply.putHeader(Message.HEADER_REFERENCES_ID, getReplyToSequenceNr());
        requireComponent(OutgoingRouter.KEY).sendMessageToMaster(getReplyToKey(), reply);
    }

    public static Intent getIntent(Context context, int cameraId, String moduleName, int replyToSequenceNumber, String replyToKey) {
        Intent intent = new Intent(context, OdroidCamera.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CAMERA_ID, cameraId);
        intent.putExtra(EXTRA_MODULE_NAME, moduleName);
        intent.putExtra(EXTRA_REPLY_TO_SEQUENCE_NR, replyToSequenceNumber);
        intent.putExtra(EXTRA_REPLY_TO_KEY, replyToKey);
        return intent;
    }

    private int getCameraID() {
        return getIntent().getIntExtra(EXTRA_CAMERA_ID, 0);
    }

    private String getModuleName() {
        return getIntent().getStringExtra(EXTRA_MODULE_NAME);
    }

    private int getReplyToSequenceNr() {
        return getIntent().getIntExtra(EXTRA_REPLY_TO_SEQUENCE_NR, -1);
    }

    private String getReplyToKey() {
        return getIntent().getStringExtra(EXTRA_REPLY_TO_KEY);
    }
}