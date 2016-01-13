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
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import de.unipassau.isl.evs.ssh.master.MasterContainer;
import de.unipassau.isl.evs.ssh.master.R;
import de.unipassau.isl.evs.ssh.core.database.AlreadyInUseException;
import de.unipassau.isl.evs.ssh.master.handler.MasterSlaveManagementHandler;

/**
 * TODO Niko: javadoc (Niko, 2016-01-05)
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
                    name = "Local Slave";
                }
                final Slave slave = new Slave(
                        name,
                        connectInfo.getID(),
                        connectInfo.getToken()
                );

                try {
                    requireComponent(MasterSlaveManagementHandler.KEY).registerSlave(slave);
                    setResult(Activity.RESULT_OK);
                    finish();
                } catch (AlreadyInUseException e) {
                    Toast.makeText(this, "Name already in use", Toast.LENGTH_SHORT).show();
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
