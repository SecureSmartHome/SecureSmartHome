package de.unipassau.isl.evs.ssh.master.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.master.MasterContainer;
import de.unipassau.isl.evs.ssh.master.R;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;
import de.unipassau.isl.evs.ssh.master.network.Server;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;

/**
 * MainActivity for the Master App
 * <p>
 * TODO Phil: build MainActivity for Master. Connection Status, own ID, connected modules and connection information.
 *
 * @author Team
 */
public class MainActivity extends MasterStartUpActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ListView slaveList;
    private ListView userDeviceList;

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
        List<Slave> slaves = new LinkedList<>();
        final SlaveController slaveController = getComponent(SlaveController.KEY);
        if (slaveController != null) {
            slaves = slaveController.getSlaves();
        }
        slaveList.setAdapter(new SlaveAdapter(slaves));

        List<UserDevice> userDevices = new LinkedList<>();
        final UserManagementController userController = getComponent(UserManagementController.KEY);
        if (userController != null) {
            userDevices = userController.getUserDevices();
        }
        userDeviceList.setAdapter(new UserDeviceAdapter(userDevices));
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
            address.setText(getAddress(id));
        }

        TextView connected = (TextView) findViewById(R.id.mainactivity_master_connected);
        connected.setText(getNumberOfConnectedClients());

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
    }

    private DeviceID getMasterID() {
        final NamingManager component = getComponent(NamingManager.KEY);
        if (component == null) {
            Log.i(TAG, "Container not yet connected.");
            return null;
        }
        return component.getMasterID();
    }

    private String getAddress(DeviceID id) {
        String address = "";
        final Server server = getComponent(Server.KEY);
        if (server == null) {
            Log.i(TAG, "Container not yet connected.");
            return address;
        }
        Channel channelMaster = server.findChannel(id);
        if (channelMaster != null) {
            address = channelMaster.localAddress().toString();
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
        List<Slave> slaves;

        public SlaveAdapter(List<Slave> slaves) {
            this.slaves = slaves;
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
            // TODO Phil
            return null;
        }
    }

    /**
     * Adapter used for {@link #userDeviceList}.
     */
    private class UserDeviceAdapter extends BaseAdapter {
        List<UserDevice> userDevices;

        public UserDeviceAdapter(List<UserDevice> userDevices) {
            this.userDevices = userDevices;
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
            //TODO Phil
            return null;
        }
    }
}
