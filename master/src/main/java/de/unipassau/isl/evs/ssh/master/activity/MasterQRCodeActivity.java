package de.unipassau.isl.evs.ssh.master.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.master.MasterContainer;
import de.unipassau.isl.evs.ssh.master.R;

/**
 * MasterQRCodeActivity to show QR-Codes in the master. This is used to the first device can be registered safely.
 *
 * @author Phil
 */
public class MasterQRCodeActivity extends BoundActivity {

    private Container container = this.getContainer();
    private Bitmap bitmap;

    public MasterQRCodeActivity() {
        super(MasterContainer.class);
    }

    private Bitmap getBitmap() {
//        return getContainer().require(QRCodeGenerator.KEY).getBitmap;
        return null;
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
        bitmap = getBitmap();

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
