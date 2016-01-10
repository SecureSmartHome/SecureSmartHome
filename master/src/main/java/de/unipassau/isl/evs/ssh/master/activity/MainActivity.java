package de.unipassau.isl.evs.ssh.master.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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

import com.google.common.collect.Lists;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import de.unipassau.isl.evs.ssh.master.MasterContainer;
import de.unipassau.isl.evs.ssh.master.R;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;
import de.unipassau.isl.evs.ssh.master.network.Server;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.DEFAULT_LOCAL_PORT;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.DEFAULT_PUBLIC_PORT;

/**
 * MainActivity for the Master App.
 * Displays connection information as well as a list of all connected slaves and user devices.
 *
 * @author Phil Werli
 */
public class MainActivity extends MasterStartUpActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ListView slaveList;
    private ListView userDeviceList;
    private UserDeviceAdapter userDeviceAdapter;
    private SlaveAdapter slaveAdapter;

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
            doUnbind();
            stopService(new Intent(this, MasterContainer.class));
            doBind();
            return true;
        } else if (id == R.id.action_refresh) {
            userDeviceAdapter.notifyDataSetChanged();
            slaveAdapter.notifyDataSetChanged();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Gets called in {@link #onContainerConnected(Container)}.
     * Builds the view components that require the container.
     */
    private void buildView() {
        TextView masterID = (TextView) findViewById(R.id.mainactivity_master_masterid);
        TextView address = (TextView) findViewById(R.id.mainactivity_master_address);

        DeviceID id = getMasterID();
        if (id != null) {
            masterID.setText(id.toShortString());
            address.setText(getMasterAddress());
        }

        TextView connected = (TextView) findViewById(R.id.mainactivity_master_connected);
        connected.setText(String.valueOf(getNumberOfConnectedClients()));

        slaveList = (ListView) findViewById(R.id.mainactivity_master_listview_slaves);
        slaveList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Slave item = (Slave) slaveList.getAdapter().getItem(position);
                final Server server = getComponent(Server.KEY);
                if (server != null) {
                    final Channel channel = server.findChannel(item.getSlaveID());
                    if (channel != null) {
                        channel.close();
                        String text = String.format(getResources().getString(R.string.closed_connection_with), item.getName());
                        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
        });

        userDeviceList = (ListView) findViewById(R.id.mainactivity_master_listview_userdevices);
        userDeviceList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                UserDevice item = (UserDevice) userDeviceList.getAdapter().getItem(position);
                final Server server = getComponent(Server.KEY);
                if (server != null) {
                    final Channel channel = server.findChannel(item.getUserDeviceID());
                    if (channel != null) {
                        channel.close();
                    }
                }
                return true;
            }
        });

        slaveAdapter = new SlaveAdapter();
        userDeviceAdapter = new UserDeviceAdapter();
        slaveList.setAdapter(slaveAdapter);
        userDeviceList.setAdapter(userDeviceAdapter);
    }

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
            if (DEFAULT_PUBLIC_PORT != DEFAULT_LOCAL_PORT) {
                bob.append("/" + DEFAULT_PUBLIC_PORT);
            }
        } else {
            final InetSocketAddress localAddress = server.getAddress();
            final InetSocketAddress publicAddress = server.getPublicAddress();
            if (!localAddress.getAddress().isAnyLocalAddress()) {
                bob.append(localAddress.getAddress().getHostAddress());
            } else {
                bob.append(ipAddress.getHostAddress());
            }
            bob.append(":").append(localAddress.getPort());
            if (publicAddress != null && publicAddress.getPort() != localAddress.getPort()) {
                bob.append("/").append(publicAddress.getPort());
            }
        }
        return bob.toString();
    }

    /**
     * @param id The device the address is returned for.
     * @return The devices network address.
     */
    private String getAddress(DeviceID id) {
        String address = "Currently not connected";
        Server server = getComponent(Server.KEY);
        if (server == null) {
            Log.i(TAG, "Container not yet connected.");
        } else {
            Channel ch = server.findChannel(id);
            if (ch != null) {
                address = ch.localAddress().toString();
            }
        }
        return address;
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
        List<Slave> slaves = new LinkedList<>();

        public SlaveAdapter() {
        }

        private void updateSlaveList() {
            SlaveController handler = getComponent(SlaveController.KEY);

            if (handler == null) {
                Log.i(TAG, "Container not yet connected!");
                return;
            }

            List<Slave> allSlaves = handler.getSlaves();

            slaves = Lists.newArrayList(allSlaves);
            Collections.sort(slaves, new Comparator<Slave>() {
                @Override
                public int compare(Slave lhs, Slave rhs) {
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
            updateSlaveList();
            super.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (slaves != null) {
                return slaves.size();
            } else {
                return 0;
            }
        }

        @Override
        public Slave getItem(int position) {
            if (slaves != null) {
                return slaves.get(position);
            } else {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            final Slave item = getItem(position);
            if (item != null && item.getName() != null) {
                return item.getName().hashCode();
            } else {
                return 0;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // the slave the view is created for
            final Slave item = getItem(position);

            LinearLayout layout;
            LayoutInflater inflater = getLayoutInflater();
            if (convertView == null) {
                layout = (LinearLayout) inflater.inflate(R.layout.slavelayout, parent, false);
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
                id.setText(slaveID.toShortString());
            }
            return layout;
        }
    }

    /**
     * Adapter used for {@link #userDeviceList}.
     */
    private class UserDeviceAdapter extends BaseAdapter {
        List<UserDevice> userDevices = new LinkedList<>();

        public UserDeviceAdapter() {
        }

        private void updateUserDeviceList() {
            UserManagementController handler = getComponent(UserManagementController.KEY);

            if (handler == null) {
                Log.i(TAG, "Container not yet connected!");
                return;
            }

            List<UserDevice> allUserDevices = handler.getUserDevices();

            userDevices = Lists.newArrayList(allUserDevices);
            Collections.sort(userDevices, new Comparator<UserDevice>() {
                @Override
                public int compare(UserDevice lhs, UserDevice rhs) {
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
            updateUserDeviceList();
            super.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (userDevices != null) {
                return userDevices.size();
            } else {
                return 0;
            }
        }

        @Override
        public UserDevice getItem(int position) {
            if (userDevices != null) {
                return userDevices.get(position);
            } else {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            final UserDevice item = getItem(position);
            if (item != null && item.getName() != null) {
                return item.getName().hashCode();
            } else {
                return 0;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // the user device the view is created for
            UserDevice item = getItem(position);

            LinearLayout layout;
            LayoutInflater inflater = getLayoutInflater();
            if (convertView == null) {
                layout = (LinearLayout) inflater.inflate(R.layout.userdevicelayout, parent, false);
            } else {
                layout = (LinearLayout) convertView;
            }

            if (item != null) {
                DeviceID userDeviceID = item.getUserDeviceID();

                TextView name = (TextView) layout.findViewById(R.id.userdevicelayout_device_name);
                name.setText(String.format(
                        getResources().getString(R.string.userdevice_name), item.getName()));

                TextView address = (TextView) layout.findViewById(R.id.userdevicelayout_device_address);
                address.setText(String.format(
                        getResources().getString(R.string.address_format), getAddress(userDeviceID)));

                TextView id = (TextView) layout.findViewById(R.id.userdevicelayout_device_id);
                id.setText(String.format(
                        getResources().getString(R.string.id_format), userDeviceID.toShortString()));
            }
            return layout;
        }
    }
}
