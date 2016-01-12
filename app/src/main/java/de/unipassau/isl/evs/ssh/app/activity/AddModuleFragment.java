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

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.dialogs.ErrorDialog;
import de.unipassau.isl.evs.ssh.app.handler.AppModifyModuleHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppModuleHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.GPIOAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.MockAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.ModuleAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.USBAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.WLANAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.ModuleType;

/**
 * This fragment allows to add new sensors to the system. If this functionality is used, a message
 * containing all needed information is generated and passed to the OutgoingRouter.
 *
 * @author Wolfgang Popp
 */
public class AddModuleFragment extends BoundFragment implements AdapterView.OnItemSelectedListener {

    private static final String TAG = AddModuleFragment.class.getSimpleName();

    private LinearLayout wlanView;
    private LinearLayout usbView;
    private LinearLayout gpioView;
    private LinearLayout mockView;

    private Button addWLANButton;
    private Button addMockButton;
    private Button addUSBButton;
    private Button addGPIOButton;

    private Spinner slaveSpinner;
    private Spinner sensorTypeSpinner;
    private EditText nameInput;

    private final AppModifyModuleHandler.NewModuleListener listener = new AppModifyModuleHandler.NewModuleListener() {
        @Override
        public void registrationFinished(final boolean wasSuccessful) {
            maybeRunOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onRegistrationFinished(wasSuccessful);
                }
            });
        }

        @Override
        public void unregistrationFinished(boolean wasSuccessful) {
        }
    };

    private void onRegistrationFinished(boolean wasSuccessful) {
        if (!wasSuccessful) {
            Toast.makeText(getActivity(), R.string.added_module_fail, Toast.LENGTH_LONG).show();
            return;
        }

        addWLANButton.setEnabled(true);
        addMockButton.setEnabled(true);
        addUSBButton.setEnabled(true);
        addGPIOButton.setEnabled(true);

        Toast.makeText(getActivity(), R.string.added_module_success, Toast.LENGTH_LONG).show();
        ((MainActivity) getActivity()).showFragmentByClass(MainFragment.class);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_addmodule, container, false);

        Spinner connectionTypeSpinner = (Spinner) view.findViewById(R.id.connection_type_spinner);
        sensorTypeSpinner = (Spinner) view.findViewById(R.id.add_module_sensor_type_spinner);
        slaveSpinner = (Spinner) view.findViewById(R.id.add_module_slave_spinner);
        nameInput = (EditText) view.findViewById(R.id.add_module_name_input);

        wlanView = createViewWLAN(view);
        usbView = createViewUSB(view);
        gpioView = createViewGPIO(view);
        mockView = createViewMock(view);

        ArrayAdapter<CharSequence> connectionTypeAdapter = ArrayAdapter.createFromResource(
                getActivity(),
                R.array.sensor_connection_types,
                android.R.layout.simple_spinner_dropdown_item);

        List<String> moduleTypes = Lists.transform(Arrays.asList(ModuleType.values()), new Function<ModuleType, String>() {
            @Override
            public String apply(ModuleType input) {
                return input.toLocalizedString(getActivity());
            }
        });

        ArrayAdapter<String> sensorTypeAdapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_spinner_dropdown_item, moduleTypes
        );

        sensorTypeSpinner.setAdapter(sensorTypeAdapter);
        connectionTypeSpinner.setAdapter(connectionTypeAdapter);
        connectionTypeSpinner.setOnItemSelectedListener(this);

        return view;
    }

    private void populateSlaveSpinner(Container container) {
        List<Slave> slaves = container.require(AppModuleHandler.KEY).getSlaves();

        if (slaves.size() < 1) {
            ArrayAdapter<String> slaveAdapter = new ArrayAdapter<>(
                    getActivity(),
                    android.R.layout.simple_spinner_dropdown_item,
                    new String[]{getResources().getString(R.string.warn_no_slave_connected)});

            slaveSpinner.setEnabled(false);
            slaveSpinner.setAdapter(slaveAdapter);
        } else {
            ArrayAdapter<Slave> slaveAdapter = new ArrayAdapter<>(
                    getActivity(), android.R.layout.simple_spinner_dropdown_item, slaves
            );

            slaveSpinner.setEnabled(true);
            slaveSpinner.setAdapter(slaveAdapter);
        }
    }

    // returns true if global input fields are filled in correctly
    private boolean checkInputFields() {
        return !nameInput.getText().toString().equals("") && slaveSpinner.isEnabled();
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        final AppModifyModuleHandler newModuleHandler = container.require(AppModifyModuleHandler.KEY);
        newModuleHandler.addNewModuleListener(listener);
        populateSlaveSpinner(container);
    }

    @Override
    public void onContainerDisconnected() {
        final AppModifyModuleHandler handler = getComponent(AppModifyModuleHandler.KEY);
        if (handler != null) {
            handler.removeNewModuleListener(listener);
        }
        super.onContainerDisconnected();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String type = parent.getItemAtPosition(position).toString();
        String[] types = getResources().getStringArray(R.array.sensor_connection_types);

        if (types[0].equals(type)) {
            wlanView.setVisibility(View.GONE);
            usbView.setVisibility(View.GONE);
            mockView.setVisibility(View.GONE);
            gpioView.setVisibility(View.VISIBLE);
        } else if (types[1].equals(type)) {
            wlanView.setVisibility(View.GONE);
            usbView.setVisibility(View.VISIBLE);
            mockView.setVisibility(View.GONE);
            gpioView.setVisibility(View.GONE);
        } else if (types[2].equals(type)) {
            wlanView.setVisibility(View.VISIBLE);
            usbView.setVisibility(View.GONE);
            mockView.setVisibility(View.GONE);
            gpioView.setVisibility(View.GONE);
        } else if (types[3].equals(type)) {
            wlanView.setVisibility(View.GONE);
            usbView.setVisibility(View.GONE);
            mockView.setVisibility(View.VISIBLE);
            gpioView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private LinearLayout createViewGPIO(View root) {
        LinearLayout gpioView = (LinearLayout) root.findViewById(R.id.addmodule_gpio);
        addGPIOButton = (Button) gpioView.findViewById(R.id.add_module_gpio_button);
        final EditText gpioPortInput = (EditText) gpioView.findViewById(R.id.add_module_gpio_port_input);

        addGPIOButton.setOnClickListener(new View.OnClickListener() {
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

    private LinearLayout createViewUSB(View root) {
        LinearLayout usbView = (LinearLayout) root.findViewById(R.id.addmodule_usb);
        addUSBButton = (Button) usbView.findViewById(R.id.add_module_usb_button);
        final EditText usbPortInput = (EditText) usbView.findViewById(R.id.add_module_usb_port_input);

        addUSBButton.setOnClickListener(new View.OnClickListener() {
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

    private LinearLayout createViewWLAN(View root) {
        LinearLayout wlanView = (LinearLayout) root.findViewById(R.id.addmodule_wlan);
        addWLANButton = (Button) wlanView.findViewById(R.id.add_module_wlan_button);
        final EditText usernameInput = (EditText) wlanView.findViewById(R.id.add_module_wlan_username_input);
        final EditText passwordInput = (EditText) wlanView.findViewById(R.id.add_module_wlan_password_input);
        final EditText portInput = (EditText) wlanView.findViewById(R.id.add_module_wlan_port_input);
        final EditText ipAdressInput = (EditText) wlanView.findViewById(R.id.add_module_wlan_ip_input);

        addWLANButton.setOnClickListener(new View.OnClickListener() {
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
                    addWLANButton.setEnabled(false);
                    addNewModule(accessPoint);
                }
            }
        });

        return wlanView;
    }


    private LinearLayout createViewMock(View root) {
        LinearLayout mockView = (LinearLayout) root.findViewById(R.id.addmodule_mock);
        addMockButton = (Button) mockView.findViewById(R.id.add_module_mock_button);

        addMockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addMockButton.setEnabled(false);
                addNewModule(new MockAccessPoint());
            }
        });

        return mockView;
    }

    private void addNewModule(ModuleAccessPoint accessPoint) {
        final AppModifyModuleHandler handler = getComponent(AppModifyModuleHandler.KEY);
        if (handler == null) {
            Log.e(TAG, "Container not connected");
            return;
        }

        String name = nameInput.getText().toString();
        DeviceID atSlave = ((Slave) slaveSpinner.getSelectedItem()).getSlaveID();
        int position = sensorTypeSpinner.getSelectedItemPosition();
        ModuleType moduleType = ModuleType.values()[position];
        Module module = new Module(name, atSlave, moduleType, accessPoint);
        handler.addNewModule(module);
    }
}