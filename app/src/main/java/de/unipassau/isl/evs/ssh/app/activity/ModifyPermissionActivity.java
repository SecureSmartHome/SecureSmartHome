package de.unipassau.isl.evs.ssh.app.activity;

import android.app.Activity;

import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * This activity allows to modify permissions of user devices. If this functionality is used a message,
 * containing all needed information, is generated and passed to the OutgoingRouter.
 */
public class ModifyPermissionActivity extends Activity implements Handler {
    @Override
    public void handle(Message message) {
        //TODO implement
        throw new UnsupportedOperationException();
    }
}