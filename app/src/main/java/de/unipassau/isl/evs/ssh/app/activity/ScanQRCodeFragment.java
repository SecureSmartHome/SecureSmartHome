package de.unipassau.isl.evs.ssh.app.activity;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;
import java.util.Queue;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.sec.QRDeviceInformation;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * ScanQRCodeFragment class to scan a QR-Code in the app UI.
 *
 * @author Phil Werli & Niko Fink
 */
public class ScanQRCodeFragment extends BoundFragment {
    private QRDeviceInformation qrScanResult;
    public static final int RESULT_OK = -1;
    private static final int REQUEST_CODE_SCAN_QR = 1;

    ////////////// @author Phil Werli ///////////////////////////////////////////////////////////
    @Override
    public void onStart() {
        super.onStart();
        requestScanQRCode();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent data) {
        if (requestCode == REQUEST_CODE_SCAN_QR) {
            if (resultCode == RESULT_OK) {
                String contents = data.getStringExtra("SCAN_RESULT");
                try {
                    final QRDeviceInformation info =
                            QRDeviceInformation.fromDataString(contents);
                    qrScanResult = info;
                    System.out.println("occ: what is going on here");
                    //((MainActivity) getActivity()).showFragmentByClass(WelcomeScreenFragment.class);
                    Intent intent = new Intent(getActivity(), WelcomeScreenFragment.class);
                    intent.putExtra(CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION, info);
                    //setResul
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), "Malformed QR-Code",
                            LENGTH_SHORT).show();
                }
            }
        }
        // forward the intent to child fragments
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        System.out.println("occ: something went down");
        System.out.println("occ: " + String.valueOf(qrScanResult == null));
        //System.out.println("occ: " + qrScanResult.getID().getIDString());

        if (qrScanResult != null) {
            System.out.println("occ: something more went down");
            //TODO: wait on container
            //TODO do something with info
            //SharedPreferences sharedPreferences =

            if (getContainer().get(ContainerService.KEY_CONTEXT) == null) {
                System.out.println("NOCONTEXT");
            } else {
                System.out.println("CONTEXT");
            }

            SharedPreferences sharedPreferences = getContainer().require(ContainerService.KEY_CONTEXT).getSharedPreferences(CoreConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(CoreConstants.NettyConstants.PREF_PORT, qrScanResult.getPort());
            StringBuilder hostNameBuilder = new StringBuilder();
            for (byte b : qrScanResult.getAddress().getAddress()) {
                hostNameBuilder.append(b).append('.');
            }
            hostNameBuilder.deleteCharAt(hostNameBuilder.length() - 1);
            System.out.println("HostNAME:" + hostNameBuilder.toString());
            editor.putString(CoreConstants.NettyConstants.PREF_HOST, hostNameBuilder.toString());
            editor.putString(CoreConstants.SharedPrefs.PREF_TOKEN, new String(qrScanResult.getToken()));
            editor.commit();
            editor.apply();
            qrScanResult = null;
        }
    }
}
