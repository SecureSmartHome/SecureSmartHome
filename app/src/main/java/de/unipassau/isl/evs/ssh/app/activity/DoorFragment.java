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
import android.widget.ImageView;

import java.lang.ref.WeakReference;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppDoorHandler;

/**
 * This activity uses the OperateDoorFragment and allows to display information contained in door messages
 * which are received from the IncomingDispatcher.
 *
 * @author Wolfgang Popp
 */
public class DoorFragment extends Fragment {

    private Button openButton;
    private Button blockButton;
    private ImageView imageView;

    private final ImageListener imageListener = new ImageListener() {
        @Override
        public void onPictureChanged(byte[] image) {
            displayImage(image);
        }
    };

    public interface ImageListener {
        void onPictureChanged(byte[] image);
    }

    @Override
    public void onStart() {
        AppDoorHandler handler = ((MainActivity) getActivity()).getContainer().require(AppDoorHandler.KEY);
        handler.addListener(imageListener);
        super.onStart();
    }

    @Override
    public void onStop() {
        AppDoorHandler handler = ((MainActivity) getActivity()).getContainer().require(AppDoorHandler.KEY);
        handler.removeListener(imageListener);
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_door, container, false);
        openButton = (Button) view.findViewById(R.id.doorFragmentOpenButton);
        blockButton = (Button) view.findViewById(R.id.doorFragmentBlockButton);
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

        updateButtons();
        return view;
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
     * executed, when the "Open" button was pressed.
     */
    private void openButtonAction() {
        AppDoorHandler handler = ((MainActivity) getActivity()).getContainer().require(AppDoorHandler.KEY);

        if (!handler.isOpen() && !handler.isBlocked()) {
            handler.openDoor();
        }

        updateButtons();
    }

    /**
     * executed, when the "Block" button was pressed.
     */
    private void blockButtonAction() {
        AppDoorHandler handler = ((MainActivity) getActivity()).getContainer().require(AppDoorHandler.KEY);

        if (handler.isBlocked()) {
            handler.unblockDoor();
        } else {
            handler.blockDoor();
        }
        updateButtons();
    }

    /**
     * Updates the buttons in this fragment's to represent the current door status.
     */
    private void updateButtons() {
        AppDoorHandler handler = ((MainActivity) getActivity()).getContainer().require(AppDoorHandler.KEY);

        if (handler.isBlocked()) {
            blockButton.setText("Unblock door");
        } else {
            blockButton.setText("Block door");
        }

        if (handler.isOpen()) {
            openButton.setEnabled(false);
            openButton.setText("Status: open");
        } else {
            openButton.setText("Open door");
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
        private int data = 0;

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