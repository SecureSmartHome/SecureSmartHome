package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * This activity allows a device to register itself by scanning a QR-Code provided by an admin device.
 * If this functionality is used a message, containing all needed information,
 * is generated and passed to the OutgoingRouter.
 */
public class AppRegisterUserDeviceFragment extends Fragment implements Handler {
    @Override
    public void handle(Message message) {
        //TODO implement
        throw new UnsupportedOperationException();
    }

    public AppRegisterUserDeviceFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_appregisteruserdevice, container, false);
    }
}