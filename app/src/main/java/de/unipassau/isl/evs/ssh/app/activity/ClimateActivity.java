package de.unipassau.isl.evs.ssh.app.activity;

import android.app.Activity;

import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * This activity allows to display information contained in climate messages which are received from
 * the IncomingDispatcher.
 * Furthermore it generates a climate messages as instructed by the UI and passes it to the OutgoingRouter.
 */
public class ClimateActivity extends Activity implements Handler {
    @Override
    public void handle(Message message) {
        //TODO implement
        throw new UnsupportedOperationException();
    }
}