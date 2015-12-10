package de.unipassau.isl.evs.ssh.app.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Set;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppModuleHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;

/**
 * This activity allows to visualize connected Modules and slaves. If this functionality is used a
 * message, requesting all needed information, is generated and passed to the OutgoingRouter.
 *
 * @author Wolfgang Popp
 */
public class StatusFragment extends BoundFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_status, container, false);
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);

        ListView slaveListView = (ListView) getActivity().findViewById(R.id.deviceStatusListView);
        ListView moduleListView = (ListView) getActivity().findViewById(R.id.deviceStatusModelsListView);

        AppModuleHandler moduleHandler = getComponent(AppModuleHandler.KEY);

        Set<Module> modules = null;
        List<Slave> slaves = null;

        if (moduleHandler != null) {
            modules = moduleHandler.getComponents();
            slaves = moduleHandler.getSlaves();
        }

        if (slaves == null || slaves.size() < 1) {
            TextView connectedSlaves = (TextView) getActivity().findViewById(R.id.deviceStatusConnectedSlaves);
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

        if (modules == null || modules.size() < 1) {
            TextView connectedModules = (TextView) getActivity().findViewById(R.id.deviceStatusConnectedModules);
            connectedModules.setText(R.string.NoModulesConnected);

        } else {
            ArrayAdapter<Module> moduleAdapter = new ArrayAdapter<Module>(getActivity().getApplicationContext(),
                    R.layout.device_status_module_item, modules.toArray(new Module[modules.size()])) {

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
    }
}