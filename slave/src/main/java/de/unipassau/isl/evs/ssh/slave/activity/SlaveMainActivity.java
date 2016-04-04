/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unipassau.isl.evs.ssh.slave.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.NamedDTO;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.network.ClientConnectionListener;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import de.unipassau.isl.evs.ssh.slave.R;
import de.unipassau.isl.evs.ssh.slave.SlaveContainer;
import de.unipassau.isl.evs.ssh.slave.handler.SlaveModuleHandler;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.DEFAULT_LOCAL_PORT;

/**
 * MainActivity for the Slave App.
 * Displays connection information as well as a list of all connected modules.
 *
 * @author Phil Werli
 */
public class SlaveMainActivity extends SlaveStartUpActivity implements ClientConnectionListener {
    private ListView moduleList;
    private final ModuleAdapter moduleListAdapter = new ModuleAdapter();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_restart) {
            doUnbind();
            stopService(new Intent(this, SlaveContainer.class));
            doBind();
            return true;
        } else if (id == R.id.action_refresh) {
            updateDisplayedData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

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
        container.require(Client.KEY).addListener(this);
    }

    @Override
    public void onContainerDisconnected() {
        updateDisplayedData();
        final Client client = getComponent(Client.KEY);
        if (client != null) {
            client.removeListener(this);
        }
    }

    /**
     * Gets called in {@link #onContainerConnected(Container)}.
     * Builds the view components that require the container.
     */
    private void buildView() {
        moduleList = (ListView) findViewById(R.id.mainactivity_slave_listview_slaves);
        moduleList.setAdapter(moduleListAdapter);

        updateDisplayedData();
    }

    private void updateDisplayedData() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final DeviceID slaveID = getSlaveID();
                if (slaveID != null) {
                    ((TextView) findViewById(R.id.mainactivity_slave_slaveid)).setText(slaveID.toShortString());
                }
                ((TextView) findViewById(R.id.mainactivity_slave_slaveaddress)).setText(getMyAddress());

                final DeviceID masterID = getMasterID();
                if (masterID != null) {
                    ((TextView) findViewById(R.id.mainactivity_slave_masterid)).setText(masterID.toShortString());
                }
                ((TextView) findViewById(R.id.mainactivity_slave_masteraddress)).setText(getMasterAddress());

                final Client client = getComponent(Client.KEY);
                int text = R.string.slave_not_connected;
                if (client != null && client.isConnectionEstablished()) {
                    text = R.string.slave_connected;
                }
                ((TextView) findViewById(R.id.mainactivity_slave_connection)).setText(text);

                moduleListAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * @return The slaves DeviceID
     */
    @Nullable
    private DeviceID getSlaveID() {
        final NamingManager namingManager = getComponent(NamingManager.KEY);
        return namingManager == null ? null : namingManager.getOwnID();
    }

    /**
     * @return The masters DeviceID
     */
    @Nullable
    private DeviceID getMasterID() {
        final NamingManager namingManager = getComponent(NamingManager.KEY);
        return namingManager == null ? null : namingManager.getMasterID();
    }


    private String getMyAddress() {
        StringBuilder bob = new StringBuilder();
        final InetAddress ipAddress = DeviceConnectInformation.findIPAddress(this);
        final Client client = getComponent(Client.KEY);
        final InetSocketAddress address = client != null ? client.getAddress() : null;
        if (address == null) {
            bob.append(ipAddress.getHostAddress());
            bob.append(":" + DEFAULT_LOCAL_PORT);
        } else if (address.getAddress().isAnyLocalAddress()) {
            bob.append(address.getAddress().getHostAddress());
            bob.append(":").append(address.getPort());
        } else {
            bob.append(address.getAddress().getHostAddress());
            bob.append(":").append(address.getPort());
        }
        return DeviceConnectInformation.trimAddress(bob.toString());
    }

    private String getMasterAddress() {
        Client client = getComponent(Client.KEY);
        if (client != null) {
            final InetSocketAddress connectedAddress = client.getAddress();
            if (connectedAddress != null) {
                return DeviceConnectInformation.trimAddress(connectedAddress.toString());
            }
            final InetSocketAddress connectAddress = client.getConnectAddress();
            if (connectAddress != null) {
                return DeviceConnectInformation.trimAddress(connectAddress.toString());
            }
        }
        return getString(R.string.no_master_address_known);
    }

    @Override
    public void onMasterFound() {
        updateDisplayedData();
    }

    @Override
    public void onClientConnecting(String host, int port) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateDisplayedData();
                ((TextView) findViewById(R.id.mainactivity_slave_connection)).setText(R.string.slave_connecting);
            }
        });
    }

    @Override
    public void onClientConnected() {
        updateDisplayedData();
    }

    @Override
    public void onClientDisconnected() {
        updateDisplayedData();
    }

    @Override
    public void onClientRejected(String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                forceStopService();
                finish();
                new AlertDialog.Builder(SlaveMainActivity.this)
                        .setTitle(R.string.slave_rejected)
                        .setMessage(R.string.warn_slave_rejected)
                        .setNeutralButton(R.string.dismiss, null)
                        .create().show();
            }
        });
    }

    /**
     * Adapter used for {@link #moduleList}.
     */
    private class ModuleAdapter extends BaseAdapter {
        private final List<Module> modules = new ArrayList<>();

        @Override
        public void notifyDataSetChanged() {
            SlaveModuleHandler handler = getComponent(SlaveModuleHandler.KEY);
            if (handler != null) {
                modules.clear();
                modules.addAll(handler.getModules());
                Collections.sort(modules, NamedDTO.COMPARATOR);
            }
            super.notifyDataSetChanged();
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
            return getItem(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // the module the view is created for
            final Module item = getItem(position);

            final LinearLayout layout;
            if (convertView == null) {
                layout = (LinearLayout) getLayoutInflater().inflate(R.layout.item_module, parent, false);
            } else {
                layout = (LinearLayout) convertView;
            }

            if (item != null) {
                TextView name = (TextView) layout.findViewById(R.id.modulelayout_module_name);
                String formattedName = String.format(getResources().getString(R.string.module_name), item.getModuleType(), item.getName());
                name.setText(formattedName);

                TextView information = (TextView) layout.findViewById(R.id.modulelayout_module_information);
                information.setText(buildLocalizedModuleInformation(item));
            }
            return layout;
        }

        /**
         * @param module A module which representation will be returned.
         * @return A localized String representation of a module.
         */
        private String buildLocalizedModuleInformation(Module module) {
            String moduleType = module.getModuleType().toLocalizedString(SlaveMainActivity.this);
            String atSlave = module.getAtSlave().toShortString();
            String moduleAccessPoint = module.getModuleAccessPoint().toString();

            return String.format(getResources().getString(R.string.module_information),
                    moduleType, atSlave, moduleAccessPoint);
        }
    }
}
