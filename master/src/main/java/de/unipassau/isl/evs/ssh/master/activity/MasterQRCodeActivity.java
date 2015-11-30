package de.unipassau.isl.evs.ssh.master.activity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.google.zxing.WriterException;

import java.io.Serializable;

import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.sec.QRDeviceInformation;
import de.unipassau.isl.evs.ssh.master.MasterContainer;
import de.unipassau.isl.evs.ssh.master.R;

/**
 * MasterQRCodeActivity to show QR-Codes in the master. This is used to the first device can be registered safely.
 *
 * @author Phil
 */
public class MasterQRCodeActivity extends BoundActivity {

    public static final String EXTRA_QR_DEVICE_INFORMATION = "EXTRA_QR_DEVICE_INFORMATION";


    private Container container = this.getContainer();
    private Bitmap bitmap;

    public MasterQRCodeActivity() {
        super(MasterContainer.class);
    }

    private Bitmap createQRCodeBitmap() {
        Serializable extra = getIntent().getExtras().getSerializable(EXTRA_QR_DEVICE_INFORMATION);
        if (extra instanceof QRDeviceInformation) {
            try {
                return ((QRDeviceInformation) extra).toQRBitmap(Bitmap.Config.ALPHA_8, Color.BLACK, Color.WHITE);
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

        if (bitmap != null) {
            imageview.setImageBitmap(bitmap);
            imageview.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
