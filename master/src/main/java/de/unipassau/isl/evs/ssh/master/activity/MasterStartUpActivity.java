package de.unipassau.isl.evs.ssh.master.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.activity.StartUpActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import de.unipassau.isl.evs.ssh.master.MasterContainer;
import de.unipassau.isl.evs.ssh.core.database.AlreadyInUseException;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;
import de.unipassau.isl.evs.ssh.master.handler.MasterRegisterDeviceHandler;
import de.unipassau.isl.evs.ssh.master.network.Server;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION;

/**
 * TODO Niko add Javadoc for whole class. (Phil, 2016-01-09)
 *
 * @author Niko Fink
 */
public class MasterStartUpActivity extends StartUpActivity {
    protected static final String PREF_PREFERENCES_SET = "master_preferences_set";

    public MasterStartUpActivity() {
        super(MasterContainer.class);
    }

    public MasterStartUpActivity(boolean bindOnStart) {
        super(MasterContainer.class, bindOnStart);
    }

    @Override
    protected boolean checkSwitchActivity() {
        if (isSwitching()) {
            return true;
        }

        final SharedPreferences prefs = getSharedPreferences(CoreConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE);
        final boolean prefsSet = prefs.getBoolean(PREF_PREFERENCES_SET, false);
        if (!prefsSet) {
            return doSwitch(MasterPreferenceActivity.class, "prefs are not set yet");
        }

        if (getContainer() != null) {
            final List<UserDevice> userDevices = getContainer().require(UserManagementController.KEY).getUserDevices();
            if (userDevices.isEmpty()) {
                return doSwitch(MasterQRCodeActivity.class, "no UserDevice is registered", new Runnable() {
                    @Override
                    public void run() {
                        startQRCodeActivity(getContainer());
                    }
                });
            } else {
                return doSwitch(MainActivity.class, "everything is set up");
            }
        }
        Log.i(TAG, "Staying in " + getClass().getSimpleName() + " as Preferences are set, but Container is not connected");
        return false;
    }

    /**
     * Build the {@link DeviceConnectInformation} and start the {@link MasterQRCodeActivity}.
     */
    private void startQRCodeActivity(Container container) {
        final SharedPreferences prefs = getSharedPreferences(CoreConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE);
        final UserDevice userDevice = new UserDevice(
                MasterRegisterDeviceHandler.FIRST_USER, MasterRegisterDeviceHandler.FIRST_GROUP,
                DeviceID.NO_DEVICE
        );
        final DeviceConnectInformation deviceInformation;
        try {
            deviceInformation = new DeviceConnectInformation(
                    DeviceConnectInformation.findIPAddress(this),
                    prefs.getInt(Server.PREF_SERVER_LOCAL_PORT, CoreConstants.NettyConstants.DEFAULT_LOCAL_PORT),
                    container.require(NamingManager.KEY).getMasterID(),
                    container.require(MasterRegisterDeviceHandler.KEY).generateNewRegisterToken(userDevice)
            );
        } catch (AlreadyInUseException e) {
            Log.wtf(TAG, "Something went wrong while getting a new register token. Apparently a user with the given "
                    + "name already exists.");
            return;
        }

        final Intent intent = new Intent(this, MasterQRCodeActivity.class);
        intent.putExtra(EXTRA_QR_DEVICE_INFORMATION, deviceInformation);
        startActivity(intent);
    }
}
