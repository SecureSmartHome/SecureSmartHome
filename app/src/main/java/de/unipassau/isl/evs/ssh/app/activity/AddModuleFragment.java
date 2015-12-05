package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.dialogs.ErrorDialog;
import de.unipassau.isl.evs.ssh.app.handler.AppModuleHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;

/**
 * This activity allows to add new sensors to the system. If this functionality is used a message,
 * containing all needed information, is generated and passed to the OutgoingRouter.
 */
public class AddModuleFragment extends BoundFragment implements AdapterView.OnItemSelectedListener {

    private static final String TAG = AddModuleFragment.class.getSimpleName();

    private LinearLayout wlanView = null;
    private LinearLayout usbView = null;
    private LinearLayout gpioView = null;

    private Spinner slaveSpinner = null;
    private EditText nameInput = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_addmodule, container, false);
        nameInput = (EditText) view.findViewById(R.id.add_module_name_input);

        Spinner connectionTypeSpinner = (Spinner) view.findViewById(R.id.connection_type_spinner);
        slaveSpinner = (Spinner) view.findViewById(R.id.slaves_spinner);

        ArrayAdapter<CharSequence> connectionTypeAdapter = ArrayAdapter.createFromResource(
                getActivity().getApplicationContext(),
                R.array.sensor_connection_types,
                android.R.layout.simple_spinner_dropdown_item);
        connectionTypeSpinner.setAdapter(connectionTypeAdapter);
        connectionTypeSpinner.setOnItemSelectedListener(this);
        return view;
    }

    // returns true if global input fields are filled in correctly
    private boolean checkInputFields(){
        return !nameInput.equals("") && slaveSpinner.isEnabled();
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        AppModuleHandler handler = getComponent(AppModuleHandler.KEY);
        if (handler == null) {
            Log.i(TAG, "Container not connected");
            return;
        }
        List<Slave> slaves = handler.getSlaves();
        ArrayAdapter<String> slaveAdapter;
        if (slaves == null) {
            slaveAdapter = new ArrayAdapter<>(
                    getActivity().getApplicationContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    new String[]{"No Slaves Connected"});

            slaveSpinner.setEnabled(false);
        } else {
            List<String> slaveNames = Lists.newArrayList(Iterables.transform(slaves, new Function<Slave, String>() {
                @Override
                public String apply(Slave input) {
                    return input.getName();
                }
            }));
            slaveAdapter = new ArrayAdapter<>(
                    getActivity().getApplicationContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    slaveNames);

            slaveSpinner.setEnabled(true);
        }

        slaveSpinner.setAdapter(slaveAdapter);


    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String type = parent.getItemAtPosition(position).toString();
        String[] types = getResources().getStringArray(R.array.sensor_connection_types);
        LinearLayout layout = (LinearLayout) getView().findViewById(R.id.add_module_layout);
        if (types[0].equals(type)) {
            createViewGPIO(layout);
        } else if (types[1].equals(type)) {
            createViewUSB(layout);
        } else if (types[2].equals(type)) {
            createViewWLAN(layout);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private void createViewGPIO(LinearLayout layout) {
        if (wlanView != null) {
            layout.removeView(wlanView);
            wlanView = null;
        }

        if (usbView != null) {
            layout.removeView(usbView);
            usbView = null;
        }

        gpioView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.addmodule_gpio, layout, false);
        Button button = (Button) gpioView.findViewById(R.id.add_module_gpio_button);
        final EditText gpioPortInput = (EditText) gpioView.findViewById(R.id.add_module_gpio_port_input);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String gpioPort = gpioPortInput.getText().toString();
                if (!checkInputFields() || gpioPort.equals("")) {
                    ErrorDialog.show(getActivity(), getActivity().getResources().getString(R.string.error_fill_all_fields));
                    return;
                }

                //TODO add module via handler
            }
        });

        layout.addView(gpioView);
    }

    private void createViewUSB(LinearLayout layout) {
        if (wlanView != null) {
            layout.removeView(wlanView);
            wlanView = null;
        }

        if (gpioView != null) {
            layout.removeView(gpioView);
            gpioView = null;
        }

        usbView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.addmodule_usb, layout, false);
        Button button = (Button) usbView.findViewById(R.id.add_module_usb_button);
        final EditText usbPortInput = (EditText) usbView.findViewById(R.id.add_module_usb_port_input);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String usbPort = usbPortInput.getText().toString();
                if (!checkInputFields() || usbPort.equals("")) {
                    ErrorDialog.show(getActivity(), getActivity().getResources().getString(R.string.error_fill_all_fields));
                    return;
                }

                //TODO add module via handler
            }
        });

        layout.addView(usbView);
    }

    private void createViewWLAN(LinearLayout layout) {
        if (gpioView != null) {
            layout.removeView(gpioView);
            gpioView = null;
        }

        if (usbView != null) {
            layout.removeView(usbView);
            usbView = null;
        }

        wlanView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.addmodule_wlan, layout, false);
        Button button = (Button) wlanView.findViewById(R.id.add_module_wlan_button);
        final EditText usernameInput = (EditText) wlanView.findViewById(R.id.add_module_wlan_username_input);
        final EditText passwordInput = (EditText) wlanView.findViewById(R.id.add_module_wlan_password_input);
        final EditText portInput = (EditText) wlanView.findViewById(R.id.add_module_wlan_port_input);
        final EditText ipAdressInput = (EditText) wlanView.findViewById(R.id.add_module_wlan_ip_input);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameInput.getText().toString();
                String password = passwordInput.getText().toString();
                String port = portInput.getText().toString();
                String ipAdress = ipAdressInput.getText().toString();

                if (!checkInputFields() || port.equals("") || ipAdress.equals("") || password.equals("") || username.equals("")) {
                    ErrorDialog.show(getActivity(), getActivity().getResources().getString(R.string.error_fill_all_fields));
                    return;
                }
                // TODO add module via handler
            }
        });

        layout.addView(wlanView);
    }
}