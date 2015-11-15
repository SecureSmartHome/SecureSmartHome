package de.unipassau.isl.evs.ssh.app.activity;

import android.app.Activity;

import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * This activity uses the OperateDoorFragment and allows to display information contained in door messages
 * which are received from the IncomingDispatcher.
 */
public class DoorActivity extends Activity implements Handler {
    @Override
    public void handle(Message message) {
        //TODO implement
        throw new UnsupportedOperationException();
    }
}