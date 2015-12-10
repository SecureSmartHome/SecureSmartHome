package de.unipassau.isl.evs.ssh.app.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.sec.QRDeviceInformation;

/**
 * WelcomeScreenFragment to display a welcome message to every user when he initially starts the app.
 * As the device isn't registered yet, it asks the user to scan a QR-Code.
 *
 * @author Phil Werli
 */
public class WelcomeScreenFragment extends ScanQRFragment {
    private QRDeviceInformation info;

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
        if (info != null) {
            SharedPreferences sharedPreferences = container.require(ContainerService.KEY_CONTEXT).getSharedPreferences(CoreConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(CoreConstants.NettyConstants.PREF_PORT, info.getPort());
            editor.putString(CoreConstants.NettyConstants.PREF_HOST, info.getAddress().getHostAddress());
            editor.putInt(CoreConstants.NettyConstants.PREF_PORT, info.getPort());
            editor.putString(CoreConstants.SharedPrefs.PREF_TOKEN, android.util.Base64.encodeToString(info.getToken(), android.util.Base64.NO_WRAP));
            editor.putString(CoreConstants.SharedPrefs.PREF_MASTER_ID, info.getID().getIDString());
            editor.commit();
            getContainer().require(Client.KEY).onDiscoverySuccessful(info.getAddress(), info.getPort());
            ((MainActivity) getActivity()).showFragmentByClass(MainFragment.class);
        }
    }

    @Override
    protected void onQRCodeScanned(QRDeviceInformation info) {
        super.onQRCodeScanned(info);
        this.info = info;
    }
}
