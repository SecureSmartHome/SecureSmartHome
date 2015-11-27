package de.unipassau.isl.evs.ssh.master;

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
import de.unipassau.isl.evs.ssh.master.network.Server;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

/**
 * MainActivitiy for the Master App
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
        super(MasterContainer.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, MasterContainer.class));
        setContentView(R.layout.activity_main);
        ((TextView) findViewById(R.id.textViewComponents)).setText(Container.components.toString());
        //((TextView) findViewById(R.id.textViewPackage)).setText(getApplicationContext().getApplicationInfo().toString());

        findViewById(R.id.buttonSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message message = new Message();
                ChannelFuture future = requireComponent(OutgoingRouter.KEY).sendMessageToMaster("/demo", message)
                        .getSendFuture();
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
        ((TextView) findViewById(R.id.textViewComponents)).setText(container.getData().toString());
        Server server = getComponent(Server.KEY);
        String status = server.getActiveChannels().size() + " connected to " + server.getAddress() + " "
                + "[" + (server.isChannelOpen() ? "open" : "closed") + ", "
                + (server.isExecutorAlive() ? "alive" : "dead") + "]";
        ((TextView) findViewById(R.id.textViewConnectionStatus)).setText(status);

        requireComponent(IncomingDispatcher.KEY).registerHandler(handler, "/demo");
    }

    @Override
    public void onContainerDisconnected() {
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
