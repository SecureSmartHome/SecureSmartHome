package de.unipassau.isl.evs.ssh.app.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppDoorHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;

import static de.unipassau.isl.evs.ssh.core.sec.Permission.LOCK_DOOR;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.TAKE_CAMERA_PICTURE;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.UNLATCH_DOOR;

/**
 * The DoorFragment provides a UI to interact with the door. This fragment provides UI components to block, unblock,
 * unlatch the door. It can display a picture from a camera that is connected to a slave in the SecureSmartHome.
 *
 * @author Wolfgang Popp
 */
public class DoorFragment extends BoundFragment {

    private static final String TAG = DoorFragment.class.getSimpleName();
    private Button openButton;
    private Button blockButton;
    private ImageView imageView;

    private final AppDoorHandler.DoorListener doorListener = new AppDoorHandler.DoorListener() {
        @Override
        public void onDoorStatusChanged() {
            maybeRunOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateButtons();
                }
            });
        }

        @Override
        public void blockActionFinished(final boolean wasSuccessful) {
            maybeRunOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (wasSuccessful) {
                        updateButtons();
                    } else {
                        Toast.makeText(getActivity(), R.string.could_not_block_door, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @Override
        public void unlatchActionFinished(final boolean wasSuccessful) {
            maybeRunOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (wasSuccessful) {
                        updateButtons();
                    } else {
                        Log.e(TAG, "Could not open door");
                        Toast.makeText(getActivity(), "Could not open door", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @Override
        public void cameraActionFinished(final boolean wasSuccessful) {
            maybeRunOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (wasSuccessful) {
                        displayImage();
                    } else {
                        Log.e(TAG, "Could not load image");
                        Toast.makeText(getActivity(), "Could not load image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_door, container, false);
        openButton = (Button) view.findViewById(R.id.doorFragmentOpenButton);
        blockButton = (Button) view.findViewById(R.id.doorFragmentBlockButton);
        ImageButton refreshButton = (ImageButton) view.findViewById(R.id.doorFragmentRefreshButton);
        imageView = (ImageView) view.findViewById(R.id.doorFragmentImageView);

        openButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openButtonAction();
            }
        });

        blockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                blockButtonAction();
            }
        });

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshImage();
            }
        });

        return view;
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        final AppDoorHandler handler = container.require(AppDoorHandler.KEY);
        handler.addListener(doorListener);


        displayImage();
        updateButtons();
    }

    @Override
    public void onContainerDisconnected() {
        AppDoorHandler doorHandler = getDoorHandler();

        if (doorHandler != null) {
            doorHandler.removeListener(doorListener);
        }

        super.onContainerDisconnected();
    }

    @Nullable
    private AppDoorHandler getDoorHandler() {
        return getComponent(AppDoorHandler.KEY);
    }

    private void refreshImage() {
        AppDoorHandler handler = getDoorHandler();

        if (handler == null) {
            Log.i(TAG, "Container not bound.");
            return;
        }
        if (((MainActivity) getActivity()).hasPermission(TAKE_CAMERA_PICTURE)) {
            handler.refreshImage();
        } else {
            Toast.makeText(getActivity(), R.string.you_can_not_request_picture, Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * executed, when the "Open" button was pressed.
     */
    private void openButtonAction() {
        AppDoorHandler handler = getDoorHandler();

        if (handler == null) {
            Log.i(TAG, "Container not bound.");
            return;
        }

        if (!handler.isOpen() && !handler.isBlocked()) {
            if (((MainActivity) getActivity()).hasPermission(UNLATCH_DOOR)) {
                handler.unlatchDoor();
            } else {
                Toast.makeText(getActivity(), R.string.you_can_not_unlatch_door, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * executed, when the "Block" button was pressed.
     */
    private void blockButtonAction() {
        AppDoorHandler handler = getDoorHandler();

        if (handler == null) {
            Log.i(TAG, "Container not bound.");
            return;
        }

        if (((MainActivity) getActivity()).hasPermission(LOCK_DOOR)) {
            if (handler.isBlocked()) {
                handler.unblockDoor();
            } else {
                handler.blockDoor();
            }
        } else {
            Toast.makeText(getActivity(), R.string.you_can_not_unblock_door, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Displays the given image on this fragment's ImageView.
     */
    private void displayImage() {
        final AppDoorHandler handler = getComponent(AppDoorHandler.KEY);

        if (handler != null) {
            byte[] image = handler.getPicture();
            if (image != null) {
                BitmapWorkerTask task = new BitmapWorkerTask(imageView);
                task.execute(image);
            }
        } else {
            Log.v(TAG, "Container not yet connected");
        }
    }

    /**
     * Updates the buttons in this fragment's to represent the current door status.
     */
    private void updateButtons() {
        AppDoorHandler handler = getDoorHandler();
        if (handler == null) {
            Log.i(TAG, "Container not bound.");
            return;
        }

        if (handler.isBlocked()) {
            blockButton.setText(R.string.unblockDoor);
        } else {
            blockButton.setText(R.string.blockDoor);
        }

        if (handler.isOpen()) {
            openButton.setEnabled(false);
            openButton.setText(R.string.doorStatusOpen);
        } else {
            openButton.setText(R.string.openDoor);
            if (handler.isBlocked()) {
                openButton.setEnabled(false);
            } else {
                openButton.setEnabled(true);
            }
        }
    }

    /**
     * Decodes a byte array image in a background task.
     */
    private class BitmapWorkerTask extends AsyncTask<byte[], Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;

        private BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(byte[]... params) {
            try (FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "evs-image.jpg"))) {
                fos.write(params[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return BitmapFactory.decodeByteArray(params[0], 0, params[0].length);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }
}