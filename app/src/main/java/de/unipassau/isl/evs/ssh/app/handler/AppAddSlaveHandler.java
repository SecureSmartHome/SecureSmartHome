package de.unipassau.isl.evs.ssh.app.handler;

import android.util.Log;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * @author Wolfgang Popp.
 */
public class AppAddSlaveHandler extends AbstractComponent implements MessageHandler {
    public static final Key<AppAddSlaveHandler> KEY = new Key<>(AppAddSlaveHandler.class);
    private static final String TAG = AppAddSlaveHandler.class.getSimpleName();

    @Override
    public void handle(Message.AddressedMessage message) {

    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {

    }

    public void registerNewSlave(DeviceID slaveID, String slaveName) {
        //TODO
        Log.v(TAG, "registerNewSlave() called");
    }
}
