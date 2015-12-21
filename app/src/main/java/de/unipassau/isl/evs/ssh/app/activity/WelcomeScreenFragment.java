package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.sec.QRDeviceInformation;

import static de.unipassau.isl.evs.ssh.core.network.Client.encodeToken;

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
            //TODO Phil: what if QR Code is Scanned before Container is connected? (Niko, 2015-12-21)
            container.require(NamingManager.KEY)
                    .setMasterID(info.getID());
            container.require(Client.KEY)
                    .onMasterFound(info.getAddress(), info.getPort(), encodeToken(info.getToken()));
            ((MainActivity) getActivity()).showFragmentByClass(MainFragment.class);
        }
    }

    @Override
    protected void onQRCodeScanned(QRDeviceInformation info) {
        this.info = info;
    }
}
