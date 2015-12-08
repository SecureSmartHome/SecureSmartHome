package de.unipassau.isl.evs.ssh.app.activity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.WriterException;

import java.io.Serializable;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.sec.QRDeviceInformation;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.QRCodeInformation.EXTRA_QR_MESSAGE;

/**
 * QRCodeFragment to display a QR-Code in the app UI. This is used to register new user-devices safely.
 *
 * @author Phil Werli
 */
public class QRCodeFragment extends Fragment {

    private Bundle bundle = this.getArguments();

    /**
     * The QR-Code which will be displayed.
     */
    private Bitmap bitmap;

    /**
     * Generates a QR-Code from the sent data.
     *
     * @return the created QR-Code
     */
    private Bitmap createQRCodeBitmap() {
        Serializable extra = bundle.getSerializable(EXTRA_QR_DEVICE_INFORMATION);
        if (extra instanceof QRDeviceInformation) {
            try {
                return ((QRDeviceInformation) extra).toQRBitmap(Bitmap.Config.ALPHA_8, Color.BLACK, Color.WHITE);
            } catch (WriterException e) {
                throw new IllegalArgumentException("illegal QR-Code data", e);
            }
        } else {
            throw new IllegalArgumentException("missing EXTRA_QR_DEVICE_INFORMATION as extra " + EXTRA_QR_DEVICE_INFORMATION);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FrameLayout root = (FrameLayout) inflater.inflate(R.layout.fragment_qrcode, container, false);
        ImageView imageview = ((ImageView) root.findViewById(R.id.qrcode_fragment_qr_code));
        bitmap = createQRCodeBitmap();

        if (bitmap != null) {
            imageview.setImageBitmap(bitmap);
            imageview.setVisibility(View.VISIBLE);
        }
        TextView textView = (TextView) root.findViewById(R.id.qrcode_fragment_text);
        String text = bundle.getString(EXTRA_QR_MESSAGE, String.valueOf(R.string.please_scan_device));
        textView.setText(text);
        return root;
    }

    @Nullable
    @Override
    public View getView() {
        return super.getView();
    }
}