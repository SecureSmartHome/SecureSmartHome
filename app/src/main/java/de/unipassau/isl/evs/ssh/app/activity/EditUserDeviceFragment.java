package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;

/**
 * EditUserDeviceFragment to view, edit and delete all information regarding a single user device.
 *
 * @author Phil Werli
 * @see ListGroupFragment
 * @see ListUserDeviceFragment
 */
public class EditUserDeviceFragment extends BoundFragment {

    private UserDevice device;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
