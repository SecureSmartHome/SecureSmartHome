package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.v7.internal.widget.AdapterViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.dialogs.ErrorDialog;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * This activity allows to add new sensors to the system. If this functionality is used a message,
 * containing all needed information, is generated and passed to the OutgoingRouter.
 */
public class AddModuleFragment extends BoundFragment implements AdapterView.OnItemSelectedListener {

    private LinearLayout wlanView = null;
    private LinearLayout usbView = null;
    private LinearLayout gpioView = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        LinearLayout view = (LinearLayout) inflater.inflate(R.layout.fragment_addmodule, container, false);

        Spinner spinner = (Spinner) view.findViewById(R.id.connection_type_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getActivity().getApplicationContext(),
                R.array.sensor_connection_types,
                android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
        return view;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String type = parent.getItemAtPosition(position).toString();
        String[] types = getResources().getStringArray(R.array.sensor_connection_types);
        if (types[0].equals(type)) {
            createViewGPIO((LinearLayout) getView());
        } else if (types[1].equals(type)) {
            createViewUSB((LinearLayout) getView());
        } else if (types[2].equals(type)) {
            createViewWLAN((LinearLayout) getView());
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

                if (port.equals("") || ipAdress.equals("") || password.equals("") || username.equals("")) {
                    ErrorDialog.show(getActivity(), getActivity().getResources().getString(R.string.error_fill_all_fields));
                    return;
                }
                // TODO add module via handler
            }
        });

        layout.addView(wlanView);
    }
}