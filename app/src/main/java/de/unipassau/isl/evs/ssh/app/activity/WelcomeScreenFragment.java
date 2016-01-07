package de.unipassau.isl.evs.ssh.app.activity;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.net.InetSocketAddress;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;

import static de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation.encodeToken;

/**
 * WelcomeScreenFragment to display a welcome message to every user when he initially starts the app.
 * As the device isn't registered yet, it asks the user to scan a QR-Code.
 *
 * @author Phil Werli
 */
public class WelcomeScreenFragment extends ScanQRFragment {
    private static final String TAG = WelcomeScreenFragment.class.getSimpleName();
    private static final String LOCAL_MASTER_PACKAGE = "de.unipassau.isl.evs.ssh.master";
    private static final String LOCAL_MASTER_ACTIVITY = LOCAL_MASTER_PACKAGE + ".activity.RegisterLocalAppActivity";
    private DeviceConnectInformation info;

    @Override
    public void onStart() {
        super.onStart();
        try {
            //Try to open the Master Activity for adding a local device
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(LOCAL_MASTER_PACKAGE, LOCAL_MASTER_ACTIVITY));
            startActivityForResult(intent, CoreConstants.QRCodeInformation.REQUEST_CODE_SCAN_QR);
        } catch (ActivityNotFoundException ignore) {
        }
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout root = (LinearLayout) inflater.inflate(R.layout.fragment_welcomescreen, container, false);

        Button button = (Button) root.findViewById(R.id.welcomescreen_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestScanQRCode();
            }
        });
        return root;
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        maybeFoundMaster();
    }

    @Override
    protected void onQRCodeScanned(DeviceConnectInformation info) {
        this.info = info;
        maybeFoundMaster();
    }

    private void maybeFoundMaster() {
        if (info != null && getContainer() != null) {

            // sets MasterID from info.
            NamingManager namingManager = getComponent(NamingManager.KEY);
            if (namingManager == null) {
                Log.i(TAG, "Container not yet connected!");
            } else {
                namingManager.setMasterID(info.getID());
            }

            // sets address from info.
            Client client = getComponent(Client.KEY);
            if (client == null) {
                Log.i(TAG, "Container not yet connected!");
            } else {
                client.onMasterFound(new InetSocketAddress(info.getAddress(), info.getPort()), encodeToken(info.getToken()));
            }
            ((MainActivity) getActivity()).showFragmentByClass(MainFragment.class);
        }
    }
}
