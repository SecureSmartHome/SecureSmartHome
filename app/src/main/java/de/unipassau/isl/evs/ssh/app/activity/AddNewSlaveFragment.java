package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.dialogs.ErrorDialog;
import de.unipassau.isl.evs.ssh.app.handler.AppAddSlaveHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;

/**
 * The AddNewSlaveFragment is used to add new Slaves to the SecureSmartHome.
 *
 * @author Wolfgang Popp
 */
public class AddNewSlaveFragment extends ScanQRFragment {
    private static final String KEY_SLAVE_NAME = "SLAVE_NAME";

    private EditText slaveNameInput;
    private DeviceConnectInformation info;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_new_slave, container, false);

        slaveNameInput = (EditText) view.findViewById(R.id.add_new_slave_name);

        if (savedInstanceState != null) {
            String slaveName = savedInstanceState.getString(KEY_SLAVE_NAME);
            if (slaveName != null) {
                slaveNameInput.setText(slaveName);
            }
        }
        return view;
    }

    private void buildView() {
        Button addNewSlaveButton = (Button) getActivity().findViewById(R.id.add_new_slave_button);

        addNewSlaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!slaveNameInput.getText().toString().equals("")) {
                    requestScanQRCode();
                } else {
                    ErrorDialog.show(getActivity(), getActivity().getResources().getString(R.string.new_slave_name_missing));
                }
            }
        });
    }

    @Override
    protected void onQRCodeScanned(DeviceConnectInformation info) {
        this.info = info;
        registerSlave();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SLAVE_NAME, slaveNameInput.getText().toString());
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        buildView();
    }

    private void registerSlave() {
        AppAddSlaveHandler handler = getComponent(AppAddSlaveHandler.KEY);

        if (info != null && handler != null) {
            DeviceID slaveID = info.getID();
            String slaveName = slaveNameInput.getText().toString();
            final byte[] passiveRegistrationToken = info.getToken();
            handler.registerNewSlave(slaveID, slaveName, passiveRegistrationToken);
            info = null;
            ((MainActivity) getActivity()).showFragmentByClass(MainFragment.class);
        } else {
            Toast toast = Toast.makeText(getActivity().getApplicationContext(), R.string.cannot_add_slave, Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}
