package de.unipassau.isl.evs.ssh.master.activity;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
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

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import de.unipassau.isl.evs.ssh.master.MasterContainer;
import de.unipassau.isl.evs.ssh.master.R;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;
import de.unipassau.isl.evs.ssh.master.handler.MasterRegisterDeviceHandler;
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
public class MainActivity extends BoundActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ListView slaveList;
    private ListView userDeviceList;

    public MainActivity() {
        super(MasterContainer.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, MasterContainer.class));
        setContentView(R.layout.activity_main);
    }

    /**
     * Checks if there are no registered devices in the system.
     *
     * @return Whether no devices are registered in the master.
     * author Phil Werli
     */
    private boolean hasNoRegisteredDevice() {
        UserManagementController component = getComponent(UserManagementController.KEY);
        if (component == null) {
            Log.i(TAG, "Container not yet connected.");
            return true;
        }
        List<UserDevice> list = component.getUserDevices();
        return list.size() == 0;
    }

    @Override
    public void onContainerConnected(Container container) {
        showRegisterQROnFirstBoot();

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

    /**
     * start MasterQRCodeActivity when no devices are registered yet
     */
    private void showRegisterQROnFirstBoot() {
        if (hasNoRegisteredDevice()) {
            Intent intent = new Intent(this, MasterQRCodeActivity.class);
            DeviceConnectInformation deviceInformation = null;
            Context ctx = requireComponent(ContainerService.KEY_CONTEXT);
            WifiManager wifiManager = ((WifiManager) ctx.getSystemService(Context.WIFI_SERVICE));
            String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
            UserDevice userDevice = new UserDevice(
                    MasterRegisterDeviceHandler.FIRST_USER, MasterRegisterDeviceHandler.NO_GROUP,
                    DeviceID.NO_DEVICE
            );
            try {
                deviceInformation = new DeviceConnectInformation(
                        (Inet4Address) Inet4Address.getByName(ipAddress),
                        CoreConstants.NettyConstants.DEFAULT_LOCAL_PORT,
                        requireComponent(NamingManager.KEY).getMasterID(),
                        requireComponent(MasterRegisterDeviceHandler.KEY).generateNewRegisterToken(userDevice)
                );
            } catch (UnknownHostException e) {
                //Todo: handle error
            }
            Log.v(TAG, "HostNAME: " + deviceInformation.getAddress().getHostAddress());
            Log.v(TAG, "ID: " + deviceInformation.getID());
            Log.v(TAG, "Port: " + deviceInformation.getPort());
            Log.v(TAG, "Token: " + android.util.Base64.encodeToString(deviceInformation.getToken(),
                    android.util.Base64.NO_WRAP));
            //NoDevice will allow any device to use this token
            intent.putExtra(CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION, deviceInformation);
            startActivity(intent);
        }
    }



    @Override
    public void onContainerDisconnected() {
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

    // FIXME use me
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
            return 404;
        }
        ChannelGroup activeChannels = server.getActiveChannels();
        return activeChannels.size();
    }

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
