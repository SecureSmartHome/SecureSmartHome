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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Strings;

import java.io.Serializable;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.AlreadyInUseException;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import de.unipassau.isl.evs.ssh.master.MasterContainer;
import de.unipassau.isl.evs.ssh.master.R;
import de.unipassau.isl.evs.ssh.master.handler.MasterSlaveManagementHandler;

/**
 * Activity that can be started by the Slave if it detected that a Master is also running on the same device.
 * Asks the user if the Slave should be registered or not.
 *
 * @author Niko Fink
 */
public class RegisterLocalSlaveActivity extends BoundActivity {
    private boolean userAccepted = false;

    public RegisterLocalSlaveActivity() {
        super(MasterContainer.class);
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        maybeAddAndFinish();
    }

    private void maybeAddAndFinish() {
        if (userAccepted && getContainer() != null) {
            final Serializable serializable = getIntent().getSerializableExtra(CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION);
            if (serializable instanceof DeviceConnectInformation) {
                final DeviceConnectInformation connectInfo = (DeviceConnectInformation) serializable;

                final TextView inputName = (TextView) findViewById(R.id.inputName);
                String name = inputName.getText().toString().trim();
                if (Strings.isNullOrEmpty(name)) {
                    name = getString(R.string.local_slave);
                }
                final Slave slave = new Slave(
                        name,
                        connectInfo.getID(),
                        connectInfo.getToken()
                );

                try {
                    getContainer().require(MasterSlaveManagementHandler.KEY).registerSlave(slave);
                    setResult(Activity.RESULT_OK);
                    finish();
                } catch (AlreadyInUseException e) {
                    Toast.makeText(this, getString(R.string.name_in_use), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_local_slave);

        findViewById(R.id.buttonRegister).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userAccepted = true;
                maybeAddAndFinish();
            }
        });
        findViewById(R.id.buttonIgnore).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        setResult(Activity.RESULT_CANCELED);
        setFinishOnTouchOutside(true);
    }
}
