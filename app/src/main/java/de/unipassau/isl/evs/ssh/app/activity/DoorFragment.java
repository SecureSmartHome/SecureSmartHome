package de.unipassau.isl.evs.ssh.app.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppDoorHandler;

/**
 * This fragment allows to display information contained in door messages
 * which are received from the IncomingDispatcher.
 *
 * @author Wolfgang Popp
 */
public class DoorFragment extends Fragment {

    private Button openButton;
    private Button blockButton;
    private ImageButton refreshButton;
    private ImageView imageView;

    private final DoorListener doorListener = new DoorListener() {
        @Override
        public void onPictureChanged(byte[] image) {
            displayImage(image);
        }

        @Override
        public void onDoorStatusChanged() {
            updateButtons();
        }
    };

    @Override
    public void onStart() {
        getDoorHandler().addListener(doorListener);
        super.onStart();
    }

    @Override
    public void onStop() {
        getDoorHandler().removeListener(doorListener);
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_door, container, false);
        openButton = (Button) view.findViewById(R.id.doorFragmentOpenButton);
        blockButton = (Button) view.findViewById(R.id.doorFragmentBlockButton);
        refreshButton = (ImageButton) view.findViewById(R.id.doorFragmentRefreshButton);
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

        getDoorHandler().refresh();
        updateButtons();
        return view;
    }

    private AppDoorHandler getDoorHandler(){
        return ((MainActivity) getActivity()).getContainer().require(AppDoorHandler.KEY);
    }

    /**
     * executed, when the "Open" button was pressed.
     */
    private void openButtonAction() {
        AppDoorHandler handler = getDoorHandler();

        if (!handler.isOpen() && !handler.isBlocked()) {
            handler.openDoor();
        }
        updateButtons();
    }

    /**
     * executed, when the "Block" button was pressed.
     */
    private void blockButtonAction() {
        AppDoorHandler handler = getDoorHandler();

        if (handler.isBlocked()) {
            handler.unblockDoor();
        } else {
            handler.blockDoor();
        }
        updateButtons();
    }

    private void refreshImage() {
        getDoorHandler().refreshImage();

    }

    /**
     * Displays the given image on this fragment's ImageView.
     *
     * @param image the image to display as byte[]
     */
    private void displayImage(byte[] image) {
        BitmapWorkerTask task = new BitmapWorkerTask(imageView);
        task.execute(image);
    }

    /**
     * Updates the buttons in this fragment's to represent the current door status.
     */
    private void updateButtons() {
        AppDoorHandler handler = getDoorHandler();

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

    public interface DoorListener {
        void onPictureChanged(byte[] image);
        void onDoorStatusChanged();
    }

    /**
     * Decodes a byte array image in a background task.
     */
    private class BitmapWorkerTask extends AsyncTask<byte[], Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;

        private BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(byte[]... params) {
            return BitmapFactory.decodeByteArray(params[0], 0, params[0].length);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }
}