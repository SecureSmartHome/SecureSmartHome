package de.unipassau.isl.evs.ssh.slave.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.slave.R;

/**
 * MainActivity for the slave app.
 * <p/>
 * TODO Phil: build MainActivity for Slave. Connection Status, own ID and MasterID, connected modules and connection information.
 *
 * @author Team
 */
public class MainActivity extends SlaveStartUpActivity {
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

        findViewById(R.id.textViewConnectionStatus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateConnectionStatus();
            }
        });

        updateConnectionStatus();
    }

    private void updateConnectionStatus() {
        Client client = getComponent(Client.KEY);
        String status;
        if (client == null) {
            status = "disconnected";
        } else {
            status = "connected to " + client.getAddress() + " "
                    + "[" + (client.isChannelOpen() ? "open" : "closed") + "]";
        }
        ((TextView) findViewById(R.id.textViewConnectionStatus)).setText(status);
    }

    @Override
    public void onContainerDisconnected() {
        updateConnectionStatus();
    }
}
