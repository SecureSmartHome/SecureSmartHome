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

import de.unipassau.isl.evs.ssh.app.AppModuleHandler;
import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * This activity allows to visualize status information of the system. If this functionality is used a message,
 * requesting all needed information, is generated and passed to the OutgoingRouter.
 *
 * @author Wolfgang Popp
 */
public class StatusFragment extends Fragment {

    public StatusFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_status, container, false);
        ListView slaveListView = (ListView) view.findViewById(R.id.deviceStatusListView);
        ListView moduleListView = (ListView) view.findViewById(R.id.deviceStatusModelsListView);

        Container diContainer = ((BoundActivity) getActivity()).getContainer();
        AppModuleHandler moduleHandler = diContainer.require(AppModuleHandler.KEY);


        List<Module> modules = moduleHandler.getComponents();
        // Dummy data
        List<Slave> slaves = new LinkedList<>();
        Slave slave1 = new Slave("sl1", new DeviceID("1.1.1.1"));
        Slave slave2 = new Slave("sl2", new DeviceID("1.1.1.2"));
        slaves.add(slave1);
        slaves.add(slave2);

        if (slaves == null) {
            TextView connectedSlaves = (TextView) view.findViewById(R.id.deviceStatusConnectedSlaves);
            connectedSlaves.setText(R.string.NoSlavesConnected);
        } else {
            ArrayAdapter<Slave> slaveAdapter = new ArrayAdapter<Slave>(getActivity().getApplicationContext(),
                    R.layout.device_status_list_item, slaves) {

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
                    TextView slaveId = (TextView) layout.findViewById(R.id.deviceStatusSlaveId);

                    Slave slave = getItem(position);
                    slaveName.setText(slave.getName());
                    slaveId.setText(slave.getSlaveID().getId());
                    return layout;
                }
            };
            slaveListView.setAdapter(slaveAdapter);
        }

        if (modules == null) {
            TextView connectedModules = (TextView) view.findViewById(R.id.deviceStatusConnectedModules);
            connectedModules.setText(R.string.NoModulesConnected);

        } else {
            ArrayAdapter<Module> moduleAdapter = new ArrayAdapter<Module>(getActivity().getApplicationContext(),
                    R.layout.device_status_module_item, modules) {

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
                    TextView moduleType = (TextView) layout.findViewById(R.id.deviceStatusModuleType);
                    TextView moduleConnectionType = (TextView) layout.findViewById(R.id.deviceStatusConnectionType);

                    Module module = getItem(position);
                    moduleName.setText(module.getName());
                    moduleType.setText(module.getModuleType());
                    moduleConnectionType.setText(module.getModuleAccessPoint().getType());
                    return layout;
                }
            };
            moduleListView.setAdapter(moduleAdapter);
        }

        return view;
    }
}