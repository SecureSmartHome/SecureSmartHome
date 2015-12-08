package de.unipassau.isl.evs.ssh.master.activity;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.InitRegisterUserDevicePayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.sec.QRDeviceInformation;
import de.unipassau.isl.evs.ssh.master.MasterContainer;
import de.unipassau.isl.evs.ssh.master.R;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;
import de.unipassau.isl.evs.ssh.master.handler.MasterRegisterDeviceHandler;
import de.unipassau.isl.evs.ssh.master.network.Server;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.group.ChannelGroup;

/**
 * MainActivity for the Master App
 *
 * @author Team
 */
public class MainActivity extends BoundActivity {
    //Todo: intend name.
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
        public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

        }

        @Override
        public void handlerRemoved(String routingKey) {

        }
    };

    public MainActivity() {
        super(MasterContainer.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, MasterContainer.class));
        setContentView(R.layout.activity_main);

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
                final ChannelFuture future = outgoingRouter.sendMessageToMaster("/demo", message).getSendFuture();
                log("OUT:" + message.toString());
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(final ChannelFuture future) throws Exception {
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
    }

    /**
     * Checks if there are no registered devices in the system.
     *
     * @return Whether no devices are registered in the master.
     * author Phil Werli
     */
    private boolean hasNoRegisteredDevice() {
        List<UserDevice> list = getContainer().require(UserManagementController.KEY).getUserDevices();
        return list.size() == 0;
    }


    @Override
    public void onContainerConnected(Container container) {
        final NamingManager namingManager = getComponent(NamingManager.KEY);
        if (namingManager == null) {
            ((TextView) findViewById(R.id.textViewDeviceID)).setText("???");
        } else {
            ((TextView) findViewById(R.id.textViewDeviceID)).setText(
                    namingManager.getOwnID().getIDString()
            );
        }

        updateConnectionStatus();

        requireComponent(IncomingDispatcher.KEY).registerHandler(handler, "/demo");

        // start MasterQRCodeActivity when no devices are registered yet
        if (hasNoRegisteredDevice()) {
            Intent intent = new Intent(this, MasterQRCodeActivity.class);
            //TODO: create QRCodeInformation from data
            QRDeviceInformation deviceInformation = null;
            Context ctx = requireComponent(ContainerService.KEY_CONTEXT);
            WifiManager wifiManager = ((WifiManager) ctx.getSystemService(Context.WIFI_SERVICE));
            String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
            try {
                deviceInformation = new QRDeviceInformation(
                        (Inet4Address) Inet4Address.getByName(ipAddress),
                        CoreConstants.NettyConstants.DEFAULT_PORT,
                        getContainer().require(NamingManager.KEY).getMasterID(),
                        QRDeviceInformation.getRandomToken()
                );
            } catch (UnknownHostException e) {
                //Todo: handle error
            }
            System.out.println("HostNAME:" + deviceInformation.getAddress().getHostAddress());
            System.out.println("ID:" + deviceInformation.getID());
            System.out.println("Port:" + deviceInformation.getPort());
            System.out.println("Token:" + android.util.Base64.encodeToString(deviceInformation.getToken(), android.util.Base64.NO_WRAP));
            //NoDevice will allow any device to use this token
            Message message = new Message(new InitRegisterUserDevicePayload(deviceInformation.getToken(),
                    new UserDevice(MasterRegisterDeviceHandler.FIRST_USER, MasterRegisterDeviceHandler.NO_GROUP,
                            DeviceID.NO_DEVICE)));
            getContainer().require(OutgoingRouter.KEY).sendMessageLocal(CoreConstants.RoutingKeys.MASTER_REGISTER_INIT, message);
            intent.putExtra(CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION, deviceInformation);
            startActivity(intent);
        }
    }

    private void updateConnectionStatus() {
        Server server = getComponent(Server.KEY);
        String status;
        if (server == null) {
            status = "server not started";
        } else {
            final ChannelGroup channels = server.getActiveChannels();
            status = channels.size() + " connected to " + server.getAddress() + " "
                    + "[" + (server.isChannelOpen() ? "open" : "closed") + ", "
                    + (server.isExecutorAlive() ? "alive" : "dead") + "]";
            if (!channels.isEmpty()) {
                status += ":\n";
                for (Channel channel : channels) {
                    final DeviceID deviceID = channel.attr(CoreConstants.NettyConstants.ATTR_PEER_ID).get();
                    status += channel.id().asShortText() + " " + channel.remoteAddress() + " " +
                            (deviceID != null ? deviceID.toShortString() : "???") + "\n";
                }
            }
        }
        ((TextView) findViewById(R.id.textViewConnectionStatus)).setText(status);
    }

    @Override
    public void onContainerDisconnected() {
        updateConnectionStatus();
        requireComponent(IncomingDispatcher.KEY).unregisterHandler(handler, "/demo");
    }

    private void log(String text) {
        Log.v("MASTER", text);
        ((TextView) findViewById(R.id.textViewLog)).append(text);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
