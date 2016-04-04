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

package de.unipassau.isl.evs.ssh.master.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.NamedDTO;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import de.unipassau.isl.evs.ssh.master.R;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;
import de.unipassau.isl.evs.ssh.master.network.Server;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.group.ChannelGroup;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.DEFAULT_LOCAL_PORT;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.DEFAULT_PUBLIC_PORT;

/**
 * MainActivity for the Master App.
 * Displays connection information as well as a list of all connected slaves and user devices.
 *
 * @author Phil Werli
 */
public class MasterMainActivity extends MasterStartUpActivity implements Server.ServerConnectionListener {
    private static final String TAG = MasterMainActivity.class.getSimpleName();

    private ListView slaveList;
    private ListView userDeviceList;
    private final SlaveAdapter slaveAdapter = new SlaveAdapter();
    private final UserDeviceAdapter userDeviceAdapter = new UserDeviceAdapter();

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
        if (!isSwitching()) {
            buildView();
        }
        container.require(Server.KEY).addListener(this);
    }


    @Override
    public void onClientConnected(Channel channel) {
        updateDisplayedData();
    }

    @Override
    public void onClientDisonnected(Channel channel) {
        updateDisplayedData();
    }

    @Override
    public void onContainerDisconnected() {
        final Server server = getComponent(Server.KEY);
        if (server != null) {
            server.removeListener(this);
        }
        super.onContainerDisconnected();
        updateDisplayedData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, MasterPreferenceActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_restart) {
            forceRestartService();
            return true;
        } else if (id == R.id.action_refresh) {
            updateDisplayedData();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Gets called in {@link #onContainerConnected(Container)}.
     * Builds the view components that require the container.
     */
    private void buildView() {
        slaveList = (ListView) findViewById(R.id.mainactivity_master_listview_slaves);
        slaveList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Slave item = (Slave) slaveList.getAdapter().getItem(position);
                final Server server = getComponent(Server.KEY);
                if (server != null) {
                    final Channel channel = server.findChannel(item.getSlaveID());
                    if (channel != null) {
                        channel.close().addListener(newConnectionClosedListener(item.getName()));
                    }
                }
                return true;
            }
        });
        slaveList.setAdapter(slaveAdapter);

        userDeviceList = (ListView) findViewById(R.id.mainactivity_master_listview_userdevices);
        userDeviceList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final UserDevice item = (UserDevice) userDeviceList.getAdapter().getItem(position);
                final Server server = getComponent(Server.KEY);
                if (server != null) {
                    final Channel channel = server.findChannel(item.getUserDeviceID());
                    if (channel != null) {
                        channel.close().addListener(newConnectionClosedListener(item.getName()));
                    }
                }
                return true;
            }
        });
        userDeviceList.setAdapter(userDeviceAdapter);

        updateDisplayedData();
    }

    private void updateDisplayedData() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceID id = getMasterID();
                if (id != null) {
                    ((TextView) findViewById(R.id.mainactivity_master_masterid)).setText(id.toShortString());
                }
                ((TextView) findViewById(R.id.mainactivity_master_address)).setText(getMasterAddress());
                ((TextView) findViewById(R.id.mainactivity_master_connected)).setText(String.valueOf(getNumberOfConnectedClients()));

                slaveAdapter.notifyDataSetChanged();
                userDeviceAdapter.notifyDataSetChanged();
            }
        });
    }

    @NonNull
    private ChannelFutureListener newConnectionClosedListener(final String name) {
        return new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String text = String.format(getResources().getString(R.string.closed_connection_with), name);
                        Toast.makeText(MasterMainActivity.this, text, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
    }

    @Nullable
    private DeviceID getMasterID() {
        final NamingManager component = getComponent(NamingManager.KEY);
        if (component == null) {
            Log.i(TAG, "Container not yet connected.");
            return null;
        }
        return component.getMasterID();
    }

    private String getMasterAddress() {
        final InetAddress ipAddress = DeviceConnectInformation.findIPAddress(this);
        StringBuilder bob = new StringBuilder();
        final Server server = getComponent(Server.KEY);
        if (server == null) {
            Log.i(TAG, "Container not yet connected.");
            bob.append(ipAddress.getHostAddress());
            bob.append(":" + DEFAULT_LOCAL_PORT);
            //noinspection ConstantConditions
            if (DEFAULT_PUBLIC_PORT != DEFAULT_LOCAL_PORT) {
                bob.append("/" + DEFAULT_PUBLIC_PORT);
            }
        } else {
            final InetSocketAddress localAddress = server.getAddress();
            final InetSocketAddress publicAddress = server.getPublicAddress();
            if (localAddress.getAddress().isAnyLocalAddress()) {
                bob.append(ipAddress.getHostAddress());
            } else {
                bob.append(localAddress.getAddress().getHostAddress());
            }
            bob.append(":").append(localAddress.getPort());
            if (publicAddress != null && publicAddress.getPort() != localAddress.getPort()) {
                bob.append("/").append(publicAddress.getPort());
            }
        }
        return DeviceConnectInformation.trimAddress(bob.toString());
    }

    private String getAddress(DeviceID id) {
        String address = "Currently not connected";
        Server server = getComponent(Server.KEY);
        if (server == null) {
            Log.i(TAG, "Container not yet connected.");
        } else {
            final Channel ch = server.findChannel(id);
            if (ch != null) {
                address = ch.remoteAddress().toString();
            }
        }
        return DeviceConnectInformation.trimAddress(address);
    }

    private int getNumberOfConnectedClients() {
        final Server server = getComponent(Server.KEY);
        if (server == null) {
            Log.i(TAG, "Container not yet connected.");
            return 0;
        }
        ChannelGroup activeChannels = server.getActiveChannels();
        return activeChannels.size();
    }

    /**
     * Adapter used for {@link #slaveList}.
     */
    private class SlaveAdapter extends BaseAdapter {
        private final List<Slave> slaves = new ArrayList<>();

        @Override
        public void notifyDataSetChanged() {
            SlaveController handler = getComponent(SlaveController.KEY);
            if (handler != null) {
                slaves.clear();
                slaves.addAll(handler.getSlaves());
                Collections.sort(slaves, NamedDTO.COMPARATOR);
            }
            super.notifyDataSetChanged();
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
            return getItem(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // the slave the view is created for
            final Slave item = getItem(position);

            final LinearLayout layout;
            if (convertView == null) {
                layout = (LinearLayout) getLayoutInflater().inflate(R.layout.item_slave, parent, false);
            } else {
                layout = (LinearLayout) convertView;
            }

            if (item != null) {
                TextView name = (TextView) layout.findViewById(R.id.slavelayout_slave_name);
                String formattedName = String.format(getResources().getString(R.string.slave_name), item.getName());
                name.setText(formattedName);

                DeviceID slaveID = item.getSlaveID();

                TextView address = (TextView) layout.findViewById(R.id.slavelayout_slave_address);
                address.setText(getAddress(slaveID));

                TextView id = (TextView) layout.findViewById(R.id.slavelayout_slave_id);
                id.setText(String.format(
                        getResources().getString(R.string.id_format), slaveID.toShortString()));
            }
            return layout;
        }
    }

    /**
     * Adapter used for {@link #userDeviceList}.
     */
    private class UserDeviceAdapter extends BaseAdapter {
        private final List<UserDevice> userDevices = new ArrayList<>();

        @Override
        public void notifyDataSetChanged() {
            UserManagementController handler = getComponent(UserManagementController.KEY);
            if (handler != null) {
                userDevices.clear();
                userDevices.addAll(handler.getUserDevices());
                Collections.sort(userDevices, NamedDTO.COMPARATOR);
            }
            super.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return userDevices.size();
        }

        @Override
        public UserDevice getItem(int position) {
            return userDevices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // the user device the view is created for
            final UserDevice item = getItem(position);

            final LinearLayout layout;
            if (convertView == null) {
                layout = (LinearLayout) getLayoutInflater().inflate(R.layout.item_userdevice, parent, false);
            } else {
                layout = (LinearLayout) convertView;
            }

            if (item != null) {
                DeviceID userDeviceID = item.getUserDeviceID();

                TextView name = (TextView) layout.findViewById(R.id.userdevicelayout_device_name);
                name.setText(String.format(
                        getResources().getString(R.string.userdevice_name), item.getName()));

                TextView address = (TextView) layout.findViewById(R.id.userdevicelayout_device_address);
                address.setText(getAddress(userDeviceID));

                TextView id = (TextView) layout.findViewById(R.id.userdevicelayout_device_id);
                id.setText(String.format(
                        getResources().getString(R.string.id_format), userDeviceID.toShortString()));
            }
            return layout;
        }
    }
}
