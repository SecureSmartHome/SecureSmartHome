package de.unipassau.isl.evs.ssh.app.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * This activity allows to visualize status information of the system. If this functionality is used a message,
 * requesting all needed information, is generated and passed to the OutgoingRouter.
 */
public class StatusFragment extends Fragment implements MessageHandler {

    public StatusFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_status, container, false);
        ListView slaveListView = (ListView) view.findViewById(R.id.deviceStatusListView);
        ListView moduleListView = (ListView) view.findViewById(R.id.deviceStatusModelsListView);

        // Dummy data
        List<Slave> slaves = new LinkedList<>();
        List<ElectronicModules> modules = new LinkedList<>();
        Slave slave1 = new Slave("sl1", true, "1.1.1.1");
        Slave slave2 = new Slave("sl2", true, "1.1.1.2");
        slaves.add(slave1);
        slaves.add(slave2);
        modules.add(new ElectronicModules(slave1, ModuleType.GPIO, true, "Klingel"));
        modules.add(new ElectronicModules(slave1, ModuleType.GPIO, true, "Türsensor1"));
        modules.add(new ElectronicModules(slave1, ModuleType.WLAN, true, "Lampe"));

        ArrayAdapter<Slave> slaveAdapter = new ArrayAdapter<Slave>(getActivity().getApplicationContext(),
                R.layout.device_status_list_item, slaves.toArray(new Slave[2])) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TableLayout layout;
                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    layout = (TableLayout) inflater.inflate(R.layout.device_status_list_item, parent, false);
                } else {
                    layout = (TableLayout) convertView;
                }

                TextView slaveName = (TextView) layout.findViewById(R.id.deviceStatusSlaveName);
                TextView slaveStatus = (TextView) layout.findViewById(R.id.deviceStatusSlaveStatus);
                TextView slaveIP = (TextView) layout.findViewById(R.id.deviceStatusSlaveIP);

                Slave slave = getItem(position);
                slaveName.setText(slave.getName());
                slaveIP.setText(slave.getIpAdress());
                slaveStatus.setText("online");
                return layout;
            }
        };

        ArrayAdapter<ElectronicModules> moduleAdapter = new ArrayAdapter<ElectronicModules>(getActivity().getApplicationContext(),
                R.layout.device_status_module_item, modules.toArray(new ElectronicModules[3])) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TableLayout layout;
                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    layout = (TableLayout) inflater.inflate(R.layout.device_status_module_item, parent, false);
                } else {
                    layout = (TableLayout) convertView;
                }

                TextView moduleName = (TextView) layout.findViewById(R.id.deviceStatusModuleName);
                TextView moduleConnectedTo = (TextView) layout.findViewById(R.id.deviceStatusModuleConnectedTo);
                TextView moduleStatus = (TextView) layout.findViewById(R.id.deviceStatusModuleStatus);
                TextView moduleType = (TextView) layout.findViewById(R.id.deviceStatusModuleType);

                ElectronicModules module = getItem(position);
                moduleName.setText(module.getName());
                moduleConnectedTo.setText(module.getConnectedTo().getName());
                moduleStatus.setText("online");
                moduleType.setText(module.getType().toString());
                return layout;
            }
        };

        slaveListView.setAdapter(slaveAdapter);
        moduleListView.setAdapter(moduleAdapter);

        return view;
    }

    @Override
    public void handle(Message.AddressedMessage message) {

    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {

    }

    public enum ModuleType {
        GPIO, WLAN, USB
    }

    /*
    public enum ModuleType {
        GPIO("GPIO"),
        WLAN("WLAN"),
        USB("USB");

        private final String type;

        ModuleType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }
    */

    public class ElectronicModules {
        private Slave connectedTo;
        private ModuleType type;
        private boolean isOnline;
        private String name;


        public ElectronicModules(Slave connectedTo, ModuleType type, boolean isOnline, String name) {
            this.connectedTo = connectedTo;
            this.type = type;
            this.isOnline = isOnline;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Slave getConnectedTo() {
            return connectedTo;
        }

        public ModuleType getType() {
            return type;
        }

        public boolean isOnline() {
            return isOnline;
        }
    }

    public class Slave {
        private String name;
        private boolean isOnline;
        private String ipAdress;

        public Slave(String name, boolean isOnline, String ipAdress) {
            this.name = name;
            this.isOnline = isOnline;
            this.ipAdress = ipAdress;
        }

        public String getName() {
            return name;
        }

        public boolean isOnline() {
            return isOnline;
        }

        public String getIpAdress() {
            return ipAdress;
        }
    }

}