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
import android.widget.Toast;

import java.util.List;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.dialogs.ErrorDialog;
import de.unipassau.isl.evs.ssh.app.handler.AppModuleHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppNewModuleHandler;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.GPIOAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.ModuleAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.USBAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.WLANAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * This fragment allows to add new sensors to the system. If this functionality is used a message,
 * containing all needed information, is generated and passed to the OutgoingRouter.
 *
 * @author Wolfgang Popp
 */
public class AddModuleFragment extends BoundFragment implements AdapterView.OnItemSelectedListener {

    private static final String TAG = AddModuleFragment.class.getSimpleName();

    private AppNewModuleHandler.NewModuleListener listener;

    private LinearLayout wlanView;
    private LinearLayout usbView;
    private LinearLayout gpioView;

    private Spinner slaveSpinner;
    private Spinner sensorTypeSpinner;
    private EditText nameInput;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_addmodule, container, false);
        LinearLayout layout = (LinearLayout) view.findViewById(R.id.add_module_layout);

        Spinner connectionTypeSpinner = (Spinner) view.findViewById(R.id.connection_type_spinner);
        sensorTypeSpinner = (Spinner) view.findViewById(R.id.add_module_sensor_type_spinner);

        slaveSpinner = (Spinner) view.findViewById(R.id.add_module_slave_spinner);
        nameInput = (EditText) view.findViewById(R.id.add_module_name_input);
        wlanView = createViewWLAN(layout);
        usbView = createViewUSB(layout);
        gpioView = createViewGPIO(layout);

        ArrayAdapter<CharSequence> connectionTypeAdapter = ArrayAdapter.createFromResource(
                getActivity().getApplicationContext(),
                R.array.sensor_connection_types,
                android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> sensorTypeAdapter = new ArrayAdapter<CharSequence>(
                getActivity().getApplicationContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        CoreConstants.ModuleType.LIGHT,
                        CoreConstants.ModuleType.WEATHER_BOARD,
                        CoreConstants.ModuleType.DOOR_BUZZER,
                        CoreConstants.ModuleType.DOOR_SENSOR,
                        CoreConstants.ModuleType.WINDOW_SENSOR,
                        CoreConstants.ModuleType.WEBCAM,
                        CoreConstants.ModuleType.DOORBELL,
                });

        sensorTypeSpinner.setAdapter(sensorTypeAdapter);
        connectionTypeSpinner.setAdapter(connectionTypeAdapter);
        connectionTypeSpinner.setOnItemSelectedListener(this);
        return view;
    }

    // returns true if global input fields are filled in correctly
    private boolean checkInputFields() {
        return !nameInput.equals("") && slaveSpinner.isEnabled();
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        AppModuleHandler handler = getComponent(AppModuleHandler.KEY);
        AppNewModuleHandler newModuleHandler = getComponent(AppNewModuleHandler.KEY);
        if (handler == null || newModuleHandler == null) {
            Log.i(TAG, "Container not connected");
            return;
        }

        List<Slave> slaves = handler.getSlaves();

        if (slaves == null) {
            ArrayAdapter<String> slaveAdapter = new ArrayAdapter<>(
                    getActivity().getApplicationContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    new String[]{"No Slaves Connected"});

            slaveSpinner.setEnabled(false);
            slaveSpinner.setAdapter(slaveAdapter);
        } else {
            ArrayAdapter<Slave> slaveAdapter = new ArrayAdapter<>(
                    getActivity().getApplicationContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    slaves);

            slaveSpinner.setEnabled(true);
            slaveSpinner.setAdapter(slaveAdapter);
        }

        listener = new AppNewModuleHandler.NewModuleListener() {
            @Override
            public void registrationFinished(boolean wasSuccessful) {
                String text;

                if (wasSuccessful) {
                    text = getActivity().getResources().getString(R.string.added_module_success);
                } else {
                    text = getActivity().getResources().getString(R.string.added_module_fail);
                }

                Toast toast = Toast.makeText(getActivity().getApplicationContext(), text, Toast.LENGTH_LONG);
                toast.show();
            }
        };
        newModuleHandler.addNewModuleListener(listener);
    }

    @Override
    public void onContainerDisconnected() {
        getComponent(AppNewModuleHandler.KEY).removeNewModuleListener(listener);
        super.onContainerDisconnected();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String type = parent.getItemAtPosition(position).toString();
        String[] types = getResources().getStringArray(R.array.sensor_connection_types);
        LinearLayout layout = (LinearLayout) getView().findViewById(R.id.add_module_layout);
        if (types[0].equals(type)) {
            layout.removeView(wlanView);
            layout.removeView(usbView);
            layout.removeView(gpioView);
            layout.addView(gpioView);
        } else if (types[1].equals(type)) {
            layout.removeView(wlanView);
            layout.removeView(gpioView);
            layout.removeView(usbView);
            layout.addView(usbView);
        } else if (types[2].equals(type)) {
            layout.removeView(usbView);
            layout.removeView(gpioView);
            layout.removeView(wlanView);
            layout.addView(wlanView);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private LinearLayout createViewGPIO(ViewGroup container) {
        LinearLayout gpioView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.addmodule_gpio, container, false);
        Button button = (Button) gpioView.findViewById(R.id.add_module_gpio_button);
        final EditText gpioPortInput = (EditText) gpioView.findViewById(R.id.add_module_gpio_port_input);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String gpioPort = gpioPortInput.getText().toString();
                if (!checkInputFields() || gpioPort.equals("")) {
                    ErrorDialog.show(getActivity(), getActivity().getResources().getString(R.string.error_fill_all_fields));
                } else {
                    GPIOAccessPoint accessPoint = new GPIOAccessPoint(Integer.valueOf(gpioPort));
                    addNewModule(accessPoint);
                }
            }
        });

        return gpioView;
    }

    private LinearLayout createViewUSB(ViewGroup container) {
        LinearLayout usbView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.addmodule_usb, container, false);
        Button button = (Button) usbView.findViewById(R.id.add_module_usb_button);
        final EditText usbPortInput = (EditText) usbView.findViewById(R.id.add_module_usb_port_input);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String usbPort = usbPortInput.getText().toString();
                if (!checkInputFields() || usbPort.equals("")) {
                    ErrorDialog.show(getActivity(), getActivity().getResources().getString(R.string.error_fill_all_fields));
                } else {
                    USBAccessPoint accessPoint = new USBAccessPoint(Integer.valueOf(usbPort));
                    addNewModule(accessPoint);
                }
            }
        });

        return usbView;
    }

    private LinearLayout createViewWLAN(ViewGroup container) {
        LinearLayout wlanView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.addmodule_wlan, container, false);
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
                } else {
                    WLANAccessPoint accessPoint = new WLANAccessPoint(Integer.valueOf(port), username, password, ipAdress);
                    addNewModule(accessPoint);
                }
            }
        });

        return wlanView;
    }

    private void addNewModule(ModuleAccessPoint accessPoint) {
        AppNewModuleHandler handler = getComponent(AppNewModuleHandler.KEY);

        if (handler == null) {
            Log.e(TAG, "Container not connected");
            return;
        }

        String name = nameInput.getText().toString();
        DeviceID atSlave = ((Slave) slaveSpinner.getSelectedItem()).getSlaveID();
        String sensorType = (String) sensorTypeSpinner.getSelectedItem();
        Module module = new Module(name, atSlave, sensorType, accessPoint);

        handler.addNewModule(module);
    }
}