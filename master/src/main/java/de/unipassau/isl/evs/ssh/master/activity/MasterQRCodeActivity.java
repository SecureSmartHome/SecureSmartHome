package de.unipassau.isl.evs.ssh.master.activity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.WriterException;

import java.io.Serializable;

import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.sec.QRDeviceInformation;
import de.unipassau.isl.evs.ssh.master.MasterContainer;
import de.unipassau.isl.evs.ssh.master.R;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.QRCodeInformation.EXTRA_QR_MESSAGE;

/**
 * MasterQRCodeActivity to display a QR-Code in the masters UI. This is used to register the first
 * user-device safely as an admin device.
 *
 * @author Phil Werli
 */
public class MasterQRCodeActivity extends BoundActivity {
    /**
     * Used to scale the qr-code.
     */
    private static final int SCALE_QRCODE = 35;
    /**
     * The QR-Code which will be displayed.
     */
    private Bitmap bitmap;

    public MasterQRCodeActivity() {
        super(MasterContainer.class);
    }

    /**
     * Generates a QR-Code from the sent data.
     *
     * @return the created QR-Code
     */
    private Bitmap createQRCodeBitmap() {
        Serializable extra = getIntent().getExtras().getSerializable(EXTRA_QR_DEVICE_INFORMATION);
        if (extra instanceof QRDeviceInformation) {
            try {
                return ((QRDeviceInformation) extra).toQRBitmap(Bitmap.Config.ARGB_8888, Color.BLACK, Color.WHITE);
            } catch (WriterException e) {
                throw new IllegalArgumentException("illegal QRCode data", e);
            }
        } else {
            throw new IllegalArgumentException("missing EXTRA_QR_DEVICE_INFORMATION as extra " + EXTRA_QR_DEVICE_INFORMATION);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public Container getContainer() {
        return super.getContainer();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);

        ImageView imageview = ((ImageView) findViewById(R.id.qrcode_activity_qr_code));
        bitmap = createQRCodeBitmap();

        //Workaround to scale QR-Code
        //Makes bitmap bigger than the screen. The imageview adjusts the size itself.
        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * SCALE_QRCODE, bitmap.getHeight() * SCALE_QRCODE, false);

        if (bitmap != null) {
            imageview.setImageBitmap(bitmap);
            imageview.setVisibility(View.VISIBLE);
        }
        TextView textview = ((TextView) findViewById(R.id.qrcode_activity_text));
        String text = getIntent().getExtras().getString(EXTRA_QR_MESSAGE);
        if (text != null) {
            textview.setText(text);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
