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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.activity.StartUpActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.AlreadyInUseException;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import de.unipassau.isl.evs.ssh.master.MasterContainer;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;
import de.unipassau.isl.evs.ssh.master.handler.MasterRegisterDeviceHandler;
import de.unipassau.isl.evs.ssh.master.network.Server;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION;

/**
 * Base class for one of the three Master Activities that are required for the setup:
 * <ol>
 * <li>MasterPreferenceActivity, displayed first and when no Preferences are set</li>
 * <li>MasterQRCodeActivity, displayed afterwards as long as no UserDevices are registered</li>
 * <li>MainActivity, displayed as soon as the system is set up</li>
 * </ol>
 *
 * @author Niko Fink
 */
@SuppressLint("Registered")
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
                return doSwitch(MasterMainActivity.class, "everything is set up");
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
