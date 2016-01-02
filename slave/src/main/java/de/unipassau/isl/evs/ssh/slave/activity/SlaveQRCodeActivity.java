package de.unipassau.isl.evs.ssh.slave.activity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.google.zxing.WriterException;

import java.io.Serializable;

import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import de.unipassau.isl.evs.ssh.slave.R;
import de.unipassau.isl.evs.ssh.slave.SlaveContainer;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.QRCodeInformation.QR_CODE_IMAGE_SCALE;

/**
 * SlaveQRCodeActivity to display a QR-Code in the slaves UI. This is used to register new slaves to the system.
 *
 * @author Wolfgang Popp.
 */
public class SlaveQRCodeActivity extends BoundActivity {

    public SlaveQRCodeActivity() {
        super(SlaveContainer.class);
    }

    /**
     * Generates a QR-Code from the sent data.
     *
     * @return the created QR-Code
     */
    private Bitmap createQRCodeBitmap() throws IllegalArgumentException {
        Serializable extra = getIntent().getExtras().getSerializable(EXTRA_QR_DEVICE_INFORMATION);
        if (extra instanceof DeviceConnectInformation) {
            try {
                return ((DeviceConnectInformation) extra).toQRBitmap(Bitmap.Config.ARGB_8888, Color.BLACK, Color.WHITE);
            } catch (WriterException e) {
                throw new IllegalArgumentException("illegal QRCode data", e);
            }
        } else {
            throw new IllegalArgumentException("missing " + EXTRA_QR_DEVICE_INFORMATION + " as extra ");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);

        ImageView imageview = ((ImageView) findViewById(R.id.qrcode_activity_qr_code));
        Bitmap bitmap = createQRCodeBitmap();

        //Workaround to scale QR-Code
        //Makes bitmap bigger than the screen. The ImageView adjusts the size itself.
        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * QR_CODE_IMAGE_SCALE,
                bitmap.getHeight() * QR_CODE_IMAGE_SCALE, false);

        if (bitmap != null) {
            imageview.setImageBitmap(bitmap);
            imageview.setVisibility(View.VISIBLE);
        }
    }
}
