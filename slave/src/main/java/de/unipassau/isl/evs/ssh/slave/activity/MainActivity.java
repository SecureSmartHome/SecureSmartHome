package de.unipassau.isl.evs.ssh.slave.activity;

import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.common.collect.Lists;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.slave.R;
import de.unipassau.isl.evs.ssh.slave.SlaveContainer;
import de.unipassau.isl.evs.ssh.slave.handler.SlaveModuleHandler;

/**
 * MainActivity for the Slave App.
 * Displays connection information as well as a list of all connected modules.
 *
 * @author Team
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

        final NamingManager namingManager = getComponent(NamingManager.KEY);
        if (namingManager == null) {
            ((TextView) findViewById(R.id.textViewDeviceID)).setText("???");
            ((TextView) findViewById(R.id.textViewMasterID)).setText("???");
        } else {
            ((TextView) findViewById(R.id.textViewDeviceID)).setText(
                    namingManager.getOwnID().getId()
            );
            if (namingManager.isMasterKnown()) {
                ((TextView) findViewById(R.id.textViewMasterID)).setText(
                        namingManager.getMasterID().getId()
                );
            }
        }

        buildView();
        List<Module> modules = new LinkedList<>();
        final SlaveModuleHandler handler = getComponent(SlaveModuleHandler.KEY);
        if (handler != null) {
            modules = handler.getModules();
        }
        moduleList.setAdapter(new ModuleAdapter(modules));
    }

        updateConnectionStatus();
    /**
     * Gets called in {@link #onContainerConnected(Container)}.
     * Builds the view components that require the container.
     */
    private void buildView() {
//        TextView slaveIDText = (TextView) findViewById(R.id.mainactivity_slave_slaveid);
//        TextView slaveAddress = (TextView) findViewById(R.id.mainactivity_slave_slaveaddress);
//        TextView masterIDText = (TextView) findViewById(R.id.mainactivity_slave_slaveid);
//        TextView masterAddress = (TextView) findViewById(R.id.mainactivity_slave_slaveaddress);
//
//        DeviceID slaveID = getSlaveID();
//        if (slaveID != null) {
//            slaveIDText.setText(slaveID.toShortString());
//            slaveAddress.setText(getAddress(false));
//        }
//
//        DeviceID masterID = getMasterID();
//        if (masterID != null) {
//            masterIDText.setText(masterID.toShortString());
//            masterAddress.setText(getAddress(true));
//        }
//        TextView connected = (TextView) findViewById(R.id.mainactivity_slave_connected);
//        connected.setText(String.valueOf(getNumberOfConnectedClients()));
//
//        moduleList = (ListView) findViewById(R.id.mainactivity_slave_listview_slaves);
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
                    address += ":" + masterAddress.getPort();
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

    private DeviceID getMasterID() {
        final NamingManager component = getComponent(NamingManager.KEY);
        if (component == null) {
            Log.i(TAG, "Container no yet connected!");
            return null;
        }
        return component.getMasterID();
    }

    private DeviceID getSlaveID() {
        final NamingManager component = getComponent(NamingManager.KEY);
        if (component == null) {
            Log.i(TAG, "Container not yet connected.");
            return null;
        }
        return component.getOwnID();
    }

    private void showQRCodeActivity(boolean tryLocal) {
        DeviceConnectInformation deviceInformation = getDeviceConnectInformation();
        if (deviceInformation == null) {
            Log.i(TAG, "Could not create DeviceConnectInformation. Missing Container!");
            Toast.makeText(MainActivity.this, "Fatal error. Couldn't create QR-Code. Please reboot.", Toast.LENGTH_LONG).show();
            return;
        }

        if (tryLocal) {
            try {
                //Try to open the Master Activity for adding a local slave
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(LOCAL_MASTER_PACKAGE, LOCAL_MASTER_ACTIVITY));
                intent.putExtra(EXTRA_QR_DEVICE_INFORMATION, deviceInformation);
                startActivityForResult(intent, LOCAL_MASTER_REQUEST_CODE);
            } catch (ActivityNotFoundException ignore) {
                //Or open the QR Code scanner
                tryLocal = false;
            }
        }

        if (!tryLocal) {
            Intent intent = new Intent(this, SlaveQRCodeActivity.class);
            intent.putExtra(EXTRA_QR_DEVICE_INFORMATION, deviceInformation);
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOCAL_MASTER_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                //TODO Niko: Show intermediate "waiting for connection" screen and don't show RegisterLocal dialogue again (Niko, 2015-12-24)
            } else {
                showQRCodeActivity(false);
            }
        }
    }

    private DeviceConnectInformation getDeviceConnectInformation() {
        DeviceConnectInformation deviceInformation = null;
        WifiManager wifiManager = ((WifiManager) getSystemService(Context.WIFI_SERVICE));
        String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        final byte[] token = DeviceConnectInformation.getRandomToken();
        Client client = getComponent(Client.KEY);
        if (client == null) {
            Log.i(TAG, "Container not yet connected!");
        } else {
            client.editPrefs()
                    .setPassiveRegistrationToken(token)
                    .commit();
        }
        NamingManager namingManager = getComponent(NamingManager.KEY);
        if (namingManager == null) {
            Log.i(TAG, "Container not yet connected!");
        } else {
            try {
                deviceInformation = new DeviceConnectInformation(
                        (Inet4Address) Inet4Address.getByName(ipAddress),
                        CoreConstants.NettyConstants.DEFAULT_LOCAL_PORT,
                        namingManager.getOwnID(),
                        token
                );
            } catch (UnknownHostException e) {
                Log.e(TAG, "Cannot show QRCode: " + e.getMessage());
            }
            if (deviceInformation != null) {
                Log.v(TAG, "HostNAME: " + deviceInformation.getAddress().getHostAddress());
                Log.v(TAG, "ID: " + deviceInformation.getID());
                Log.v(TAG, "Port: " + deviceInformation.getPort());
                Log.v(TAG, "Token: " + encodeToken(deviceInformation.getToken()));
            }
        }
        //NoDevice will allow any device to use this token
        return deviceInformation;
    }

    private void updateConnectionStatus() {
//        Client client = getComponent(Client.KEY);
//        String status;
//        if (client == null) {
//            status = "disconnected";
//        } else {
//            status = "connected to " + client.getAddress() + " "
//                    + "[" + (client.isChannelOpen() ? "open" : "closed") + "]";
//        }
//        ((TextView) findViewById(R.id.textViewConnectionStatus)).setText(status);
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
            Module item = getItem(position);

            return null;
        }
    }
}
