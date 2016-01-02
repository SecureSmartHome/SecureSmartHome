package de.unipassau.isl.evs.ssh.slave.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import de.unipassau.isl.evs.ssh.slave.R;
import de.unipassau.isl.evs.ssh.slave.SlaveContainer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.GLOBAL_DEMO;
import static de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation.encodeToken;

/**
 * MainActivity for the slave app.
 * <p/>
 * TODO Phil: build MainActivity for Slave. Connection Status, own ID and MasterID, connected modules and connection information.
 *
 * @author Team
 */
public class MainActivity extends BoundActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private final MessageHandler handler = new MessageHandler() {
        @Override
        public void handle(final Message.AddressedMessage message) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    log("IN: " + message.toString());
                }
            });
        }

        @Override
        public void handlerAdded(IncomingDispatcher dispatcher, RoutingKey routingKey) {
        }

        @Override
        public void handlerRemoved(RoutingKey routingKey) {
        }
    };

    public MainActivity() {
        super(SlaveContainer.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, SlaveContainer.class));
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onContainerConnected(Container container) {
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

        findViewById(R.id.buttonSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Message message = new Message();
                final OutgoingRouter outgoingRouter = getComponent(OutgoingRouter.KEY);
                if (outgoingRouter == null) {
                    Toast.makeText(MainActivity.this, "Container not connected", Toast.LENGTH_SHORT).show();
                    return;
                }
                final Future<Void> future = outgoingRouter.sendMessageToMaster(GLOBAL_DEMO, message).getSendFuture();
                log("OUT:" + message.toString());
                future.addListener(new GenericFutureListener<Future<Void>>() {
                    @Override
                    public void operationComplete(final Future<Void> future) throws Exception {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast toast;
                                if (future.isSuccess()) {
                                    toast = Toast.makeText(MainActivity.this, "Message sent", Toast.LENGTH_SHORT);
                                } else {
                                    toast = Toast.makeText(MainActivity.this, "Sending failed: " + future.cause(), Toast.LENGTH_LONG);
                                }
                                toast.show();
                            }
                        });
                    }
                });
            }
        });


        updateConnectionStatus();

        requireComponent(IncomingDispatcher.KEY).registerHandler(handler, GLOBAL_DEMO);

        boolean isMasterKnown = requireComponent(NamingManager.KEY).isMasterKnown();
        if (!isMasterKnown) {
            showQRCodeActivity(true);
        }
    }

    private static final int LOCAL_MASTER_REQUEST_CODE = 2;
    private static final String LOCAL_MASTER_PACKAGE = "de.unipassau.isl.evs.ssh.master";
    private static final String LOCAL_MASTER_ACTIVITY = LOCAL_MASTER_PACKAGE + ".activity.RegisterLocalSlaveActivity";

    private void showQRCodeActivity(boolean tryLocal) {
        DeviceConnectInformation deviceInformation = getDeviceConnectInformation();

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

    @NonNull
    private DeviceConnectInformation getDeviceConnectInformation() {
        DeviceConnectInformation deviceInformation = null;
        Context ctx = getApplicationContext();
        WifiManager wifiManager = ((WifiManager) ctx.getSystemService(Context.WIFI_SERVICE));
        String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        final byte[] token = DeviceConnectInformation.getRandomToken();
        requireComponent(Client.KEY).editPrefs()
                .setPassiveRegistrationToken(token)
                .commit();
        try {
            deviceInformation = new DeviceConnectInformation(
                    (Inet4Address) Inet4Address.getByName(ipAddress),
                    CoreConstants.NettyConstants.DEFAULT_LOCAL_PORT,
                    requireComponent(NamingManager.KEY).getOwnID(),
                    token
            );
        } catch (UnknownHostException e) {
            Log.e(TAG, "Cannot show QRCode: " + e.getMessage());
        }
        Log.v(TAG, "HostNAME: " + deviceInformation.getAddress().getHostAddress());
        Log.v(TAG, "ID: " + deviceInformation.getID());
        Log.v(TAG, "Port: " + deviceInformation.getPort());
        Log.v(TAG, "Token: " + encodeToken(deviceInformation.getToken()));
        //NoDevice will allow any device to use this token
        return deviceInformation;
    }

    private void updateConnectionStatus() {
        Client client = getComponent(Client.KEY);
        String status;
        if (client == null) {
            status = "disconnected";
        } else {
            status = "connected to " + client.getAddress() + " "
                    + "[" + (client.isChannelOpen() ? "open" : "closed") + ", "
                    + (client.isExecutorAlive() ? "alive" : "dead") + "]";
        }
        ((TextView) findViewById(R.id.textViewConnectionStatus)).setText(status);
    }

    @Override
    public void onContainerDisconnected() {
        updateConnectionStatus();
        requireComponent(IncomingDispatcher.KEY).unregisterHandler(handler, GLOBAL_DEMO);
    }

    private void log(String text) {
        Log.v("SLAVE", text);
        ((TextView) findViewById(R.id.textViewLog)).append(text);
    }
}
