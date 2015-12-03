package de.unipassau.isl.evs.ssh.app.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import de.unipassau.isl.evs.ssh.app.AppConstants;
import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.sec.QRDeviceInformation;

/**
 * WelcomeScreenFragment to display a welcome message to every user when he initially starts the app.
 * As the device isn't registered yet, it asks the user to scan a QR-Code by calling {@link ScanQRCodeFragment}.
 *
 * @author Phil Werli
 */
public class WelcomeScreenFragment extends BoundFragment {

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        System.out.println("occ: hit ocv");
        LinearLayout root = (LinearLayout) inflater.inflate(R.layout.fragment_welcomescreen, container, false);

        Button button = (Button) root.findViewById(R.id.welcomescreen_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).showFragmentByClass(ScanQRCodeFragment.class);
            }
        });
        return root;
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        System.out.println("occ: hit occ");
        Bundle bundle = getArguments();
        QRDeviceInformation info = null;
        if (bundle != null) {
            info = (QRDeviceInformation) bundle.get(AppConstants.Fragment_Arguments.ARGUMENT_FRAGMENT);
        }
        if (info != null) {
            System.out.println("occ: something went down");
            System.out.println("occ: " + String.valueOf(info == null));
            //System.out.println("occ: " + qrScanResult.getID().getIDString());

            if (info != null) {
                System.out.println("occ: something more went down");
                //TODO: wait on container
                //TODO do something with info
                //SharedPreferences sharedPreferences =

                SharedPreferences sharedPreferences = getContainer().require(ContainerService.KEY_CONTEXT).getSharedPreferences(CoreConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(CoreConstants.NettyConstants.PREF_PORT, info.getPort());
                StringBuilder hostNameBuilder = new StringBuilder();
                for (byte b : info.getAddress().getAddress()) {
                    hostNameBuilder.append(b + 128).append('.');
                }
                hostNameBuilder.deleteCharAt(hostNameBuilder.length() - 1);
                System.out.println("HostNAME:" + hostNameBuilder.toString());
                System.out.println("ID:" + info.getID());
                System.out.println("Port:" + info.getPort());
                System.out.println("Token:" + new String(info.getToken()));
                editor.putString(CoreConstants.NettyConstants.PREF_HOST, hostNameBuilder.toString());
                editor.putInt(CoreConstants.NettyConstants.PREF_PORT, info.getPort());
                editor.putString(CoreConstants.SharedPrefs.PREF_TOKEN, new String(info.getToken()));
                editor.putString(CoreConstants.SharedPrefs.PREF_MASTER_ID, info.getID().getIDString());
                editor.commit();
                editor.apply();
            }
        }
    }
}
