package de.unipassau.isl.evs.ssh.app.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.container.Container;

/**
 * This fragment only displays a QR-Code other devices can scan.
 *
 * @author Phil
 */
public class QRCodeFragment extends Fragment {

    Bitmap bitmap;

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private Bitmap getBitmap() {
//        return getContainer().require(QRCodeGenerator.KEY).getBitmap;
        return null;
    }

    private Container getContainer() {
        return ((MainActivity) getActivity()).getContainer();
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FrameLayout root = (FrameLayout) inflater.inflate(R.layout.fragment_qrcode, container, false);
        ImageView imageview = ((ImageView) root.findViewById(R.id.qrcode_fragment_qr_code));
        bitmap = getBitmap();

        if (bitmap != null) {
            imageview.setImageBitmap(bitmap);
            imageview.setVisibility(View.VISIBLE);
        }
        return root;
    }

    @Nullable
    @Override
    public View getView() {
        return super.getView();
    }
}
