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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.util.Log;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.activity.StartUpActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import de.unipassau.isl.evs.ssh.slave.SlaveContainer;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION;

/**
 * Base class for one of the two Slave Activities that are required for the setup:
 * <ol>
 * <li>SlaveQRCodeActivity, displayed first and as long as no Master is known</li>
 * <li>MainActivity, displayed as soon as the system is set up</li>
 * </ol>
 *
 * @author Niko Fink
 */
@SuppressLint("Registered")
public class SlaveStartUpActivity extends StartUpActivity {
    public SlaveStartUpActivity() {
        super(SlaveContainer.class);
    }

    public SlaveStartUpActivity(boolean bindOnStart) {
        super(SlaveContainer.class, bindOnStart);
    }

    protected boolean checkSwitchActivity() {
        if (isSwitching()) {
            return true;
        }

        final Container container = getContainer();
        if (container != null) {
            boolean isMasterKnown = container.require(NamingManager.KEY).isMasterKnown();
            if (!isMasterKnown) {
                return doSwitch(SlaveQRCodeActivity.class, "Master is not known yet", new Runnable() {
                    @Override
                    public void run() {
                        startQRCodeActivity(container);
                    }
                });
            } else {
                return doSwitch(SlaveMainActivity.class, "everything is set up");
            }
        }

        Log.i(TAG, "Staying in " + getClass().getSimpleName() + " as Container is not connected");
        return false;
    }

    /**
     * Build the {@link DeviceConnectInformation} and start the {@link SlaveQRCodeActivity}.
     */
    private void startQRCodeActivity(Container container) {
        final byte[] token = DeviceConnectInformation.getRandomToken();
        container.require(Client.KEY).editPrefs()
                .setPassiveRegistrationToken(token)
                .commit();
        final DeviceConnectInformation deviceInformation = new DeviceConnectInformation(
                DeviceConnectInformation.findIPAddress(this),
                CoreConstants.NettyConstants.DEFAULT_LOCAL_PORT,
                container.require(NamingManager.KEY).getOwnID(),
                token
        );

        Intent intent = new Intent(this, SlaveQRCodeActivity.class);
        intent.putExtra(EXTRA_QR_DEVICE_INFORMATION, deviceInformation);
        startActivity(intent);
    }
}
