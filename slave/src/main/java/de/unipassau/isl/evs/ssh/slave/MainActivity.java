package de.unipassau.isl.evs.ssh.slave;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

/**
 * MainActivity for the slave app.
 *
 * @author Team
 */
public class MainActivity extends BoundActivity {
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
        super(SlaveContainer.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, SlaveContainer.class));
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

    @Override
    public void onContainerConnected(Container container) {
        final NamingManager namingManager = getComponent(NamingManager.KEY);
        if (namingManager == null) {
            ((TextView) findViewById(R.id.textViewDeviceID)).setText("???");
            ((TextView) findViewById(R.id.textViewMasterID)).setText("???");
        } else {
            ((TextView) findViewById(R.id.textViewDeviceID)).setText(
                    namingManager.getLocalDeviceId().getId()
            );
            ((TextView) findViewById(R.id.textViewMasterID)).setText(
                    namingManager.getMasterID().getId()
            );
        }

        updateConnectionStatus();

        requireComponent(IncomingDispatcher.KEY).registerHandler(handler, "/demo");
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
        requireComponent(IncomingDispatcher.KEY).unregisterHandler(handler, "/demo");
    }

    private void log(String text) {
        Log.v("SLAVE", text);
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
