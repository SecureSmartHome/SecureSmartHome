package de.unipassau.isl.evs.ssh.app.activity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
public class QRCodeFragment extends BoundFragment {

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
        Serializable extra = getArguments().getSerializable(EXTRA_QR_DEVICE_INFORMATION);
        if (extra instanceof QRDeviceInformation) {
            try {
                return ((QRDeviceInformation) extra).toQRBitmap(Bitmap.Config.ARGB_8888, Color.BLACK, Color.WHITE);
            } catch (WriterException e) {
                throw new IllegalArgumentException("illegal QR-Code data", e);
            }
        } else {
            throw new IllegalArgumentException("missing EXTRA_QR_DEVICE_INFORMATION as extra " + EXTRA_QR_DEVICE_INFORMATION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout root = (LinearLayout) inflater.inflate(R.layout.fragment_qrcode, container, false);
        ImageView imageview = ((ImageView) root.findViewById(R.id.qrcode_fragment_qr_code));
        bitmap = createQRCodeBitmap();
        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 8, bitmap.getHeight() * 8, false);

        if (bitmap != null) {
            imageview.setImageBitmap(bitmap);
            imageview.setVisibility(View.VISIBLE);
        }
        TextView textView = (TextView) root.findViewById(R.id.qrcode_fragment_text);
        String text = getArguments().getString(EXTRA_QR_MESSAGE, getResources().getString(R.string.please_scan_device));
        textView.setText(text);
        return root;
    }
}
