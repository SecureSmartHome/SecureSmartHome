package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.dialogs.ErrorDialog;
import de.unipassau.isl.evs.ssh.app.handler.AppSlaveManagementHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import de.unipassau.isl.evs.ssh.core.sec.Permission;

/**
 * The AddNewSlaveFragment is used to add new Slaves to the SecureSmartHome.
 *
 * @author Wolfgang Popp
 */
public class AddNewSlaveFragment extends ScanQRFragment {
    private static final String KEY_SLAVE_NAME = "SLAVE_NAME";
    private final AppSlaveManagementHandler.SlaveManagementListener listener = new AppSlaveManagementHandler.SlaveManagementListener() {
        @Override
        public void onSlaveRegistered(final boolean wasSuccessful) {
            maybeRunOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (wasSuccessful) {
                        showToast(R.string.slave_registration_success);
                        ((AppMainActivity) getActivity()).showFragmentByClass(MainFragment.class);
                    } else {
                        showToast(R.string.cannot_add_slave);
                    }
                }
            });
        }

        @Override
        public void onSlaveRemoved(boolean wasSuccessful) {
            //this fragment does not handle slave removal
        }
    };
    private EditText slaveNameInput;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_new_slave, container, false);
        slaveNameInput = (EditText) view.findViewById(R.id.add_new_slave_name);
        Button addNewSlaveButton = (Button) view.findViewById(R.id.add_new_slave_button);

        addNewSlaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!slaveNameInput.getText().toString().equals("")) {
                    requestScanQRCode();
                } else {
                    ErrorDialog.show(getActivity(), getResources().getString(R.string.new_slave_name_missing));
                }
            }
        });

        if (savedInstanceState != null) {
            String slaveName = savedInstanceState.getString(KEY_SLAVE_NAME);
            if (slaveName != null) {
                slaveNameInput.setText(slaveName);
            }
        }

        return view;
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        container.require(AppSlaveManagementHandler.KEY).addSlaveManagementListener(listener);
    }

    @Override
    public void onContainerDisconnected() {
        final AppSlaveManagementHandler handler = getComponent(AppSlaveManagementHandler.KEY);
        if (handler != null) {
            handler.removeSlaveManagementListener(listener);
        }
        super.onContainerDisconnected();
    }

    @Override
    protected void onQRCodeScanned(DeviceConnectInformation info) {
        AppSlaveManagementHandler handler = getComponent(AppSlaveManagementHandler.KEY);

        if (info != null && handler != null && ((AppMainActivity) getActivity()).hasPermission(Permission.ADD_ODROID)) {
            final DeviceID slaveID = info.getID();
            final String slaveName = slaveNameInput.getText().toString();
            final byte[] passiveRegistrationToken = info.getToken();

            handler.registerNewSlave(slaveID, slaveName, passiveRegistrationToken);
        } else {
            showToast(R.string.cannot_add_slave);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_SLAVE_NAME, slaveNameInput.getText().toString());
        super.onSaveInstanceState(outState);
    }
}
