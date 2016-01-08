package de.unipassau.isl.evs.ssh.app.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;

import java.util.List;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppModifyModuleHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppModuleHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppSlaveManagementHandler;
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
    private static final String TAG = StatusFragment.class.getSimpleName();

    private ListView slaveListView;
    private ListView moduleListView;
    private TextView connectedSlavesText;
    private TextView connectedModulesText;

    private final AppModuleHandler.AppModuleListener listener = new AppModuleHandler.AppModuleListener() {
        @Override
        public void onModulesRefreshed() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    update();
                }
            });
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status, container, false);
        slaveListView = (ListView) view.findViewById(R.id.deviceStatusListView);
        moduleListView = (ListView) view.findViewById(R.id.deviceStatusModulesListView);
        connectedSlavesText = (TextView) view.findViewById(R.id.deviceStatusConnectedSlaves);
        connectedModulesText = (TextView) view.findViewById(R.id.deviceStatusConnectedModules);

        return view;
    }

    @Override
    public void onContainerConnected(final Container container) {
        super.onContainerConnected(container);

        AppModuleHandler handler = container.require(AppModuleHandler.KEY);
        handler.addAppModuleListener(listener);

        moduleListView.setAdapter(new ModuleAdapter(handler.getComponents()));
        slaveListView.setAdapter(new SlaveAdapter(handler.getSlaves()));

        moduleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Module module = (Module) moduleListView.getItemAtPosition(position);
                String message = String.format(getResources().getString(R.string.delete_confirmation), module.getName());

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(message)
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (getContainer() != null) {
                                    getContainer().require(AppModifyModuleHandler.KEY).removeModule(module);
                                }

                            }
                        })
                        .setNegativeButton(R.string.cancel, null).create().show();
            }
        });

        slaveListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Slave slave = (Slave) slaveListView.getItemAtPosition(position);
                String message = String.format(getResources().getString(R.string.delete_confirmation), slave.getName());

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(message)
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (getContainer() != null) {
                                    getContainer().require(AppSlaveManagementHandler.KEY).deleteSlave(slave.getSlaveID());
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancel, null).create().show();

            }
        });

        update();
    }

    @Override
    public void onContainerDisconnected() {
        super.onContainerDisconnected();

        AppModuleHandler handler = getComponent(AppModuleHandler.KEY);
        if (handler != null) {
            handler.removeAppModuleListener(listener);
        }
    }

    private void update() {
        AppModuleHandler handler = getComponent(AppModuleHandler.KEY);
        if (handler == null) {
            Log.e(TAG, "update(): Container is not connected");
            return;
        }
        List<Slave> slaves = handler.getSlaves();
        if (slaves.size() < 1) {
            connectedSlavesText.setText(R.string.NoSlavesConnected);
        } else {
            connectedSlavesText.setText(R.string.deviceStatusConnectedSlaves);
        }

        List<Module> modules = handler.getComponents();
        if (modules.size() < 1) {
            connectedModulesText.setText(R.string.NoModulesConnected);
        } else {
            connectedModulesText.setText(R.string.deviceStatusConnectedModules);
        }

        ((ModuleAdapter) moduleListView.getAdapter()).update(handler.getComponents());
        ((SlaveAdapter) slaveListView.getAdapter()).update(handler.getSlaves());
    }

    private class SlaveAdapter extends BaseAdapter {

        private List<Slave> slaves;

        private SlaveAdapter(List<Slave> slaves) {
            this.slaves = slaves;
        }

        public void update(List<Slave> slaves) {
            this.slaves = slaves;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return slaves.size();
        }

        @Override
        public Slave getItem(int position) {
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
            slaveId.setText(slave.getSlaveID().toShortString());
            return layout;
        }
    }

    private class ModuleAdapter extends BaseAdapter {

        private List<Module> modules;

        public ModuleAdapter(List<Module> modules) {
            this.modules = modules;
        }

        public void update(List<Module> modules) {
            this.modules = modules;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return modules.size();
        }

        @Override
        public Module getItem(int position) {
            return modules.get(position);
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
            moduleType.setText(module.getModuleType().toLocalizedString(getActivity()));
            moduleConnectionType.setText(module.getModuleAccessPoint().getType());
            return layout;
        }
    }
}