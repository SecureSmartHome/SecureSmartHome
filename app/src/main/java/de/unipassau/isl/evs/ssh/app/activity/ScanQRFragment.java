/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
                } catch (IOException | RuntimeException e) {
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
