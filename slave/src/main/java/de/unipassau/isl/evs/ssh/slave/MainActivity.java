package de.unipassau.isl.evs.ssh.slave;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
import de.unipassau.isl.evs.ssh.core.network.Client;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public class MainActivity extends BoundActivity {
    public MainActivity() {
        super(SlaveContainer.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, SlaveContainer.class));
        setContentView(R.layout.activity_main);
        ((TextView) findViewById(R.id.textViewComponents)).setText(Container.components.toString());
        //((TextView) findViewById(R.id.textViewPackage)).setText(getApplicationContext().getApplicationInfo().toString());

        findViewById(R.id.buttonSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message message = new Message();
                ChannelFuture future = requireComponent(OutgoingRouter.KEY).sendMessageToMaster("/demo", message);
                ((TextView) findViewById(R.id.textViewLog)).append("OUT:" + message.toString());
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
        Client client = getComponent(Client.KEY);
        String status = "connected to " + client.getAddress() + " "
                + "[" + (client.isChannelOpen() ? "open" : "closed") + ", "
                + (client.isExecutorAlive() ? "alive" : "dead") + "]";
        ((TextView) findViewById(R.id.textViewConnectionStatus)).setText(status);

        requireComponent(IncomingDispatcher.KEY).registerHandler(new MessageHandler() {
            @Override
            public void handle(final Message.AddressedMessage message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView) findViewById(R.id.textViewLog)).append("IN: " + message.toString());
                    }
                });
            }

            @Override
            public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

            }

            @Override
            public void handlerRemoved(String routingKey) {

            }
        }, "/demo");
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
