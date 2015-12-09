package de.unipassau.isl.evs.ssh.slave.activity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.WriterException;

import java.io.Serializable;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Container; import de.unipassau.isl.evs.ssh.core.sec.QRDeviceInformation;
import de.unipassau.isl.evs.ssh.slave.R;
import de.unipassau.isl.evs.ssh.slave.SlaveContainer;

/**
 * @author Wolfgang Popp.
 */
public class SlaveQRCodeActivity extends BoundActivity {
    /**
     * The QR-Code which will be displayed.
     */
    private Bitmap bitmap;

    public SlaveQRCodeActivity() {
        super(SlaveContainer.class);
    }

    /**
     * Generates a QR-Code from the sent data.
     *
     * @return the created QR-Code
     */
    private Bitmap createQRCodeBitmap() {
        Serializable extra = getIntent().getExtras().getSerializable(CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION);
        if (extra instanceof QRDeviceInformation) {
            try {
                return ((QRDeviceInformation) extra).toQRBitmap(Bitmap.Config.ARGB_8888, Color.BLACK, Color.WHITE);
            } catch (WriterException e) {
                throw new IllegalArgumentException("illegal QRCode data", e);
            }
        } else {
            throw new IllegalArgumentException("missing " + CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION + " as extra ");
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
        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 8, bitmap.getHeight() * 8, false);

        if (bitmap != null) {
            imageview.setImageBitmap(bitmap);
            imageview.setVisibility(View.VISIBLE);
        }
        TextView textview = ((TextView) findViewById(R.id.qrcode_activity_text));
        String text = getIntent().getExtras().getString(CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION);
        if (text != null) {
            textview.setText(text);
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
