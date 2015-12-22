package de.unipassau.isl.evs.ssh.app.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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

    private AppModuleHandler handler;
    private ListView slaveListView;
    private ListView moduleListView;
    private TextView connectedSlavesText;
    private TextView connectedModulesText;

    AppModuleHandler.AppModuleListener listener = new AppModuleHandler.AppModuleListener() {
        @Override
        public void onModulesRefreshed() {
            update();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_status, container, false);
        slaveListView = (ListView) getActivity().findViewById(R.id.deviceStatusListView);
        moduleListView = (ListView) getActivity().findViewById(R.id.deviceStatusModulesListView);
        connectedSlavesText = (TextView) getActivity().findViewById(R.id.deviceStatusConnectedSlaves);
        connectedModulesText = (TextView) getActivity().findViewById(R.id.deviceStatusConnectedModules);
        return view;
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        handler = container.require(AppModuleHandler.KEY);
        handler.addAppModuleListener(listener);

        moduleListView.setAdapter(new ModuleAdapter());
        slaveListView.setAdapter(new SlaveAdapter());

        update();
    }

    @Override
    public void onContainerDisconnected() {
        super.onContainerDisconnected();
        handler.removeAppModuleListener(listener);
    }

    private void update() {
        List<Slave> slaves = handler.getSlaves();
        if (slaves == null || slaves.size() < 1) {
            connectedSlavesText.setText(R.string.NoSlavesConnected);
        } else {
            connectedSlavesText.setText(R.string.deviceStatusConnectedSlaves);
        }

        Set<Module> modules = handler.getComponents();
        if (modules == null || modules.size() < 1) {
            connectedModulesText.setText(R.string.NoModulesConnected);
        } else {
            connectedModulesText.setText(R.string.deviceStatusConnectedModules);
        }

        ((ModuleAdapter) moduleListView.getAdapter()).update();
        ((SlaveAdapter) moduleListView.getAdapter()).update();
    }

    private class SlaveAdapter extends BaseAdapter {

        private List<Slave> slaves;

        public SlaveAdapter() {
            slaves = handler.getSlaves();
        }

        public void update() {
            slaves = handler.getSlaves();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (handler == null) {
                return 0;
            }
            return slaves.size();
        }

        @Override
        public Slave getItem(int position) {
            if (handler == null) {
                return null;
            }
            return slaves.get(position);
        }

        @Override
        public long getItemId(int position) {
            final Slave slave = getItem(position);
            if (slave != null && slave.getName() != null) {
                return slave.getName().hashCode();
            }
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TableLayout layout;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                layout = (TableLayout) inflater.inflate(R.layout.device_status_list_item, parent, false);
            } else {
                layout = (TableLayout) convertView;
            }

            TextView slaveName = (TextView) layout.findViewById(R.id.deviceStatusSlaveName);
            TextView slaveId = (TextView) layout.findViewById(R.id.deviceStatusSlaveId);

            Slave slave = getItem(position);
            slaveName.setText(slave.getName());
            slaveId.setText(slave.getSlaveID().getIDString());
            return layout;
        }
    }

    private class ModuleAdapter extends BaseAdapter {

        Module[] modules;

        public ModuleAdapter() {
            modules = handler.getComponents().toArray(new Module[handler.getComponents().size()]);
        }

        public void update() {
            modules = handler.getComponents().toArray(new Module[handler.getComponents().size()]);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return modules.length;
        }

        @Override
        public Module getItem(int position) {
            return modules[position];
        }

        @Override
        public long getItemId(int position) {
            final Module module = getItem(position);
            if (module != null && module.getName() != null) {
                return module.getName().hashCode();
            }
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TableLayout layout;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
    }
}