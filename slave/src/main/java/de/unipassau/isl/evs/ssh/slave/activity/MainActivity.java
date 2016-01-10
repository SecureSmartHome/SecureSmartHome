package de.unipassau.isl.evs.ssh.slave.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.collect.Lists;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.slave.R;
import de.unipassau.isl.evs.ssh.slave.handler.SlaveModuleHandler;

/**
 * MainActivity for the Slave App.
 * Displays connection information as well as a list of all connected modules.
 *
 * @author Phil Werli
 */
public class MainActivity extends SlaveStartUpActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ListView moduleList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isSwitching()) {
            setContentView(R.layout.activity_main);
        }
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        if (isSwitching()) {
            return;
        }

        buildView();
        List<Module> modules = new LinkedList<>();
        final SlaveModuleHandler handler = getComponent(SlaveModuleHandler.KEY);
        if (handler != null) {
            modules = handler.getModules();
        }
        moduleList.setAdapter(new ModuleAdapter(modules));
    }

    /**
     * Gets called in {@link #onContainerConnected(Container)}.
     * Builds the view components that require the container.
     */
    private void buildView() {
        TextView slaveIDText = (TextView) findViewById(R.id.mainactivity_slave_slaveid);
        TextView slaveAddress = (TextView) findViewById(R.id.mainactivity_slave_slaveaddress);

        DeviceID slaveID = getSlaveID();
        if (slaveID != null) {
            slaveIDText.setText(slaveID.toShortString());
            slaveAddress.setText(getAddress(false));
        }
        moduleList = (ListView) findViewById(R.id.mainactivity_slave_listview_slaves);

        updateConnectionStatus();
    }

    /**
     * @param fromMaster Return address from master or not.
     * @return The devices network address.
     */
    private String getAddress(boolean fromMaster) {
        String address = "Currently not connected";
        Client client = getComponent(Client.KEY);
        if (client == null) {
            Log.i(TAG, "Container not yet connected.");
        } else {
            if (fromMaster) {
                InetSocketAddress masterAddress = client.getConnectAddress();
                if (masterAddress != null) {
                    address = masterAddress.toString();
                }
            } else {
                InetSocketAddress slaveAddress = client.getAddress();
                if (slaveAddress != null) {
                    address = slaveAddress.toString();
                }
            }
        }
        return address;
    }

    /**
     * @return The slaves DeviceID
     */
    private DeviceID getSlaveID() {
        final NamingManager namingManager = getComponent(NamingManager.KEY);
        if (namingManager == null) {
            Log.i(TAG, "Container not yet connected.");
            return null;
        }
        return namingManager.getOwnID();
    }

    /**
     * Update connection status and en/disables the visibility of the master connection
     */
    private void updateConnectionStatus() {
        Client client = getComponent(Client.KEY);
        String status;
        if (client == null) {
            status = "not connected.";

            DeviceID masterID = getMasterID();
            if (masterID != null) {
                TextView masterIDText = (TextView) findViewById(R.id.mainactivity_slave_masterid);
                TextView masterAddress = (TextView) findViewById(R.id.mainactivity_slave_masteraddress);
                masterIDText.setText(masterID.toShortString());
                masterAddress.setText(getAddress(true));
            }
            findViewById(R.id.mainactivity_slave_mastercontainer_id).setVisibility(View.VISIBLE);
            findViewById(R.id.mainactivity_slave_mastercontainer_address).setVisibility(View.VISIBLE);
        } else {
            status = "connected to";
            findViewById(R.id.mainactivity_slave_mastercontainer_id).setVisibility(View.GONE);
            findViewById(R.id.mainactivity_slave_mastercontainer_address).setVisibility(View.GONE);
        }
        ((TextView) findViewById(R.id.mainactivity_slave_connected)).setText(status);
    }

    /**
     * @return The masters DeviceID
     */
    private DeviceID getMasterID() {
        final NamingManager namingManager = getComponent(NamingManager.KEY);
        if (namingManager == null) {
            Log.i(TAG, "Container no yet connected!");
            return null;
        }
        return namingManager.getMasterID();
    }

    @Override
    public void onContainerDisconnected() {
        updateConnectionStatus();
    }

    /**
     * Adapter used for {@link #moduleList}.
     */
    private class ModuleAdapter extends BaseAdapter {
        private List<Module> modules;

        public ModuleAdapter(List<Module> modules) {
            this.modules = modules;
        }

        private void updateModuleList() {
            SlaveModuleHandler handler = getComponent(SlaveModuleHandler.KEY);

            if (handler == null) {
                Log.i(TAG, "Container not yet connected!");
                return;
            }

            List<Module> allModules = handler.getModules();

            modules = Lists.newArrayList(allModules);
            Collections.sort(modules, new Comparator<Module>() {
                @Override
                public int compare(Module lhs, Module rhs) {
                    if (lhs.getName() == null) {
                        return rhs.getName() == null ? 0 : 1;
                    }
                    if (rhs.getName() == null) {
                        return -1;
                    }
                    return lhs.getName().compareTo(rhs.getName());
                }
            });
        }

        @Override
        public void notifyDataSetChanged() {
            updateModuleList();
            super.notifyDataSetChanged();
        }


        @Override
        public int getCount() {
            if (modules != null) {
                return modules.size();
            } else {
                return 0;
            }
        }

        @Override
        public Module getItem(int position) {
            if (modules != null) {
                return modules.get(position);
            } else {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            final Module item = getItem(position);
            if (item != null && item.getName() != null) {
                return item.getName().hashCode();
            } else {
                return 0;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // the module the view is created for
            final Module item = getItem(position);

            LinearLayout layout;
            LayoutInflater inflater = getLayoutInflater();
            if (convertView == null) {
                layout = (LinearLayout) inflater.inflate(R.layout.modulelayout, parent, false);
            } else {
                layout = (LinearLayout) convertView;
            }

            if (item != null) {
                TextView name = (TextView) layout.findViewById(R.id.modulelayout_module_name);
                String formattedName = String.format(getResources().getString(R.string.module_name), item.getModuleType(), item.getName());
                name.setText(formattedName);

                TextView information = (TextView) layout.findViewById(R.id.modulelayout_module_information);
                information.setText(item.toString());
            }
            return layout;
        }
    }
}
