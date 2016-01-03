package de.unipassau.isl.evs.ssh.app.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.io.IOException;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;

/**
 * The ScanQRFragment is a BoundFragment that can be extended to communicate with a QR-Code Scanner
 * Activity. When extending this Fragment use {@code requestScanQRCode()} to start the QR-Code
 * Scanner Activity and {@code onQRCodeScanned()} to receive the scan result.
 *
 * @author Wolfgang Popp.
 */
public abstract class ScanQRFragment extends BoundFragment {
    /**
     * Starts the QR-Code Scanner or an app store to install a QR-Code Scanner, if no scanner is
     * installed.
     */
    protected void requestScanQRCode() {
        try {
            // Try to open ZXing to scan the QR-Cde
            startActivityForResult(CoreConstants.QRCodeInformation.ZXING_SCAN_INTENT, CoreConstants.QRCodeInformation.REQUEST_CODE_SCAN_QR);
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

        if (requestCode == CoreConstants.QRCodeInformation.REQUEST_CODE_SCAN_QR) {
            if (resultCode == Activity.RESULT_OK) {
                String contents = data.getStringExtra(CoreConstants.QRCodeInformation.ZXING_SCAN_RESULT);
                try {
                    final DeviceConnectInformation info = DeviceConnectInformation.fromDataString(contents);
                    onQRCodeScanned(info);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), R.string.malformed_qr_code, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Called when the Qr-Code was scanned successfully.
     *
     * @param info the information contained in the QR-Code
     */
    protected abstract void onQRCodeScanned(DeviceConnectInformation info);
}
