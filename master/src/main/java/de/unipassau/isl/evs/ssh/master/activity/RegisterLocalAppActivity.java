package de.unipassau.isl.evs.ssh.master.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.common.base.Strings;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import de.unipassau.isl.evs.ssh.master.MasterContainer;
import de.unipassau.isl.evs.ssh.master.R;
import de.unipassau.isl.evs.ssh.core.database.AlreadyInUseException;
import de.unipassau.isl.evs.ssh.master.handler.MasterRegisterDeviceHandler;
import de.unipassau.isl.evs.ssh.master.network.Server;

/**
 * TODO Niko: javadoc (Niko, 2016-01-05)
 *
 * @author Niko Fink
 */
public class RegisterLocalAppActivity extends BoundActivity {
    private boolean userAccepted = false;

    public RegisterLocalAppActivity() {
        super(MasterContainer.class);
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        maybeFinishWithResult();
    }

    private void maybeFinishWithResult() {
        if (userAccepted && getContainer() != null) {
            final TextView inputName = (TextView) findViewById(R.id.inputName);
            String name = inputName.getText().toString().trim();
            if (Strings.isNullOrEmpty(name)) {
                name = "Local App";
            }
            UserDevice userDevice = new UserDevice(
                    name, MasterRegisterDeviceHandler.NO_GROUP, DeviceID.NO_DEVICE
            );

            final Inet4Address address;
            try {
                address = (Inet4Address) InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
            } catch (UnknownHostException e) {
                throw new AssertionError("Could not lookup 127.0.0.1", e);
            }
            DeviceConnectInformation deviceInfo = null;
            try {
                deviceInfo = new DeviceConnectInformation(
                        address,
                        ((InetSocketAddress) requireComponent(Server.KEY).getAddress()).getPort(),
                        requireComponent(NamingManager.KEY).getMasterID(),
                        requireComponent(MasterRegisterDeviceHandler.KEY).generateNewRegisterToken(userDevice)
                );
            } catch (AlreadyInUseException e) {
                //TODO someone: Handle exception. This exception occurs when the given name is already registered. (Leon, 13.01.16)
                return;
            }

            final Intent data = new Intent();
            data.putExtra(CoreConstants.QRCodeInformation.ZXING_SCAN_RESULT, deviceInfo.toDataString());
            setResult(RESULT_OK, data);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_local_app);

        findViewById(R.id.buttonRegister).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userAccepted = true;
                maybeFinishWithResult();
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
