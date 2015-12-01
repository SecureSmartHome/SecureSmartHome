package de.unipassau.isl.evs.ssh.app.activity;

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import de.unipassau.isl.evs.ssh.app.R;

/**
 * ScanQRCodeFragment class to scan a QR-Code in the app UI.
 *
 * @author Phil Werli & Niko Fink
 */
public class ScanQRCodeFragment extends Fragment {

    private static final int REQUEST_CODE_SCAN_QR = 1;

    ////////////// @author Phil Werli ///////////////////////////////////////////////////////////
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
        LinearLayout root = (LinearLayout) inflater.inflate(R.layout.fragment_scan_qrcode, container, false);

        return root;
    }

    @Nullable
    @Override
    public View getView() {
        return super.getView();
    }

    ////////////// @author Niko Fink //////////////////////////////////////////////////////////////
    private void requestScanQRCode() {
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
/*
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == REQUEST_CODE_SCAN_QR) {
            if (resultCode == RESULT_OK) {
                String contents = data.getStringExtra("SCAN_RESULT");
                try {
                    final QRDeviceInformation info =
                            QRDeviceInformation.fromDataString(contents);
                    //TODO do something with info
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Malformed QR-Code",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
        // forward the intent to child fragments
        super.onActivityResult(requestCode, resultCode, data);
    }
    */
}
