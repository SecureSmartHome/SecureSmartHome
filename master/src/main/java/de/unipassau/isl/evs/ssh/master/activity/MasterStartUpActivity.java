package de.unipassau.isl.evs.ssh.master.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import de.unipassau.isl.evs.ssh.master.MasterConstants;
import de.unipassau.isl.evs.ssh.master.MasterContainer;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;
import de.unipassau.isl.evs.ssh.master.handler.MasterRegisterDeviceHandler;
import de.unipassau.isl.evs.ssh.master.network.Server;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION;

/**
 * TODO Niko add Javadoc for whole class. (Phil, 2016-01-09)
 *
 * @author Niko Fink
 */
public class MasterStartUpActivity extends BoundActivity {
    protected static final String PREF_PORT_LOCAL = "master_port_local";
    protected static final String PREF_PORT_INTERN = "master_port_intern";
    protected static final String PREF_PORT_EXTERN = "master_port_extern";
    protected static final String PREF_CITY_NAME = "master_city_name";
    protected static final String PREF_PREFERENCES_SET = "master_preferences_set";
    private static final String TAG = MasterStartUpActivity.class.getSimpleName();
    private boolean switching = false;

    public MasterStartUpActivity() {
        super(MasterContainer.class);
    }

    public MasterStartUpActivity(boolean bindOnStart) {
        super(MasterContainer.class, bindOnStart);
    }

    public boolean isSwitching() {
        return isFinishing() || switching;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkSwitchActivity();
    }

    @Override
    public void onContainerConnected(Container container) {
        checkSwitchActivity();
    }

    private void finishLater() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            finish();
        }
        switching = true;
    }

    public boolean checkSwitchActivity() {
        if (isSwitching()) {
            return true;
        }

        final SharedPreferences prefs = getSharedPreferences(MasterConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE);
        final boolean prefsSet = prefs.getBoolean(PREF_PREFERENCES_SET, false);
        if (!prefsSet) {
            if (getClass().equals(MasterPreferenceActivity.class)) {
                Log.v(TAG, "Staying in MasterPreferenceActivity as prefs are not set yet");
                return false;
            } else {
                startActivity(new Intent(this, MasterPreferenceActivity.class));
                finishLater();
                Log.i(TAG, "Switching to MasterPreferenceActivity as prefs a not set yet");
                return true;
            }
        }

        if (getContainer() != null) {
            final List<UserDevice> userDevices = getContainer().require(UserManagementController.KEY).getUserDevices();
            if (userDevices.isEmpty()) {
                if (getClass().equals(MasterQRCodeActivity.class)) {
                    Log.v(TAG, "Staying in MasterQRCodeActivity as no UserDevice is registered");
                    return false;
                } else {
                    startQRCodeActivity(getContainer());
                    finishLater();
                    Log.i(TAG, "Switching to MasterQRCodeActivity as no UserDevice is registered");
                    return true;
                }
            } else {
                if (getClass().equals(MainActivity.class)) {
                    Log.v(TAG, "Staying in MainActivity as everything is set up");
                    return false;
                } else {
                    startActivity(new Intent(this, MainActivity.class));
                    finishLater();
                    Log.i(TAG, "Switching to MainActivity as everything is set up");
                    return true;
                }
            }
        }
        Log.i(TAG, "Staying in " + getClass().getSimpleName() + " as Preferences are set, but Container is not connected");
        return false;
    }

    /**
     * Build the {@link DeviceConnectInformation} and start the {@link MasterQRCodeActivity}.
     */
    private void startQRCodeActivity(Container container) {
        final SharedPreferences prefs = getSharedPreferences(MasterConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE);
        final UserDevice userDevice = new UserDevice(
                MasterRegisterDeviceHandler.FIRST_USER, MasterRegisterDeviceHandler.NO_GROUP,
                DeviceID.NO_DEVICE
        );

        final Intent intent = new Intent(this, MasterQRCodeActivity.class);
        final DeviceConnectInformation deviceInformation = new DeviceConnectInformation(
                getIPAddress(),
                prefs.getInt(Server.PREF_SERVER_LOCAL_PORT, MasterConstants.NettyConstants.DEFAULT_LOCAL_PORT),
                container.require(NamingManager.KEY).getMasterID(),
                container.require(MasterRegisterDeviceHandler.KEY).generateNewRegisterToken(userDevice)
        );
        intent.putExtra(EXTRA_QR_DEVICE_INFORMATION, deviceInformation);
        startActivity(intent);
    }

    /**
     * Android has no uniform way of finding the IP address of the local device, so this first queries the WifiManager
     * and afterwards tries to find a suitable NetworkInterface. If both fails, returns 0.0.0.0.
     */
    @SuppressWarnings("unchecked")
    private InetAddress getIPAddress() {
        WifiManager wifiManager = ((WifiManager) getSystemService(Context.WIFI_SERVICE));
        final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        if (connectionInfo != null) {
            try {
                return InetAddress.getByName(
                        Formatter.formatIpAddress(connectionInfo.getIpAddress())
                );
            } catch (UnknownHostException e) {
                Log.wtf(getClass().getSimpleName(), "Android API couldn't resolve the IP Address of the local device", e);
            }
        }
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr.getAddress().length == DeviceConnectInformation.ADDRESS_LENGTH) {
                        return addr;
                    }
                }
            }
        } catch (SocketException e) {
            Log.wtf(getClass().getSimpleName(), "Android API query own network interfaces", e);
        }
        try {
            return InetAddress.getByAddress(new byte[DeviceConnectInformation.ADDRESS_LENGTH]);
        } catch (UnknownHostException e) {
            throw new AssertionError("Could not resolve IP 0.0.0.0", e);
        }
    }
}
