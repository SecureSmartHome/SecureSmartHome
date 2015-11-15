package de.unipassau.isl.evs.ssh.app.activity;

import android.app.Activity;

import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * This fragment is used by the door activity and allows to open or lock the door.
 * If this functionality is used a message, containing all needed information,
 * is generated and passed to the OutgoingRouter.
 */
public class OperateDoorFragment extends Activity implements Handler {
    @Override
    public void handle(Message message) {
        //TODO implement
        throw new UnsupportedOperationException();
    }
}