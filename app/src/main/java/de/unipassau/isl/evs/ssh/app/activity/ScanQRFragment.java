package de.unipassau.isl.evs.ssh.app.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.io.IOException;

import de.unipassau.isl.evs.ssh.core.sec.QRDeviceInformation;

/**
 * TODO add javadoc
 *
 * @author Wolfgang Popp.
 */
public class ScanQRFragment extends BoundFragment {
    public static final String SCAN_RESULT = "SCAN_RESULT";
    protected static final int REQUEST_CODE_SCAN_QR = 1;

    protected void requestScanQRCode() {
        try {
            // Try to open ZXing to scan the QR-Cde
            Intent intent = new
                    Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(intent, REQUEST_CODE_SCAN_QR);
        } catch (ActivityNotFoundException e) {
            // If it's not installed, open the Play Store and let the user install it
            Uri marketUri =
                    Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
            startActivity(marketIntent);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SCAN_QR) {
            if (resultCode == Activity.RESULT_OK) {
                String contents = data.getStringExtra(SCAN_RESULT);
                try {
                    final QRDeviceInformation info = QRDeviceInformation.fromDataString(contents);
                    onQRCodeScanned(info);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), "Malformed QR-Code", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    protected void onQRCodeScanned(QRDeviceInformation info) {

    }
}
