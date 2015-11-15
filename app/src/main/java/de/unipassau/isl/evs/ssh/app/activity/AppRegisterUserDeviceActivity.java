package de.unipassau.isl.evs.ssh.app.activity;

import android.app.Activity;

import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * This activity allows a device to register itself by scanning a QR-Code provided by an admin device.
 * If this functionality is used a message, containing all needed information,
 * is generated and passed to the OutgoingRouter.
 */
public class AppRegisterUserDeviceActivity extends Activity implements Handler {
    @Override
    public void handle(Message message) {
        //TODO implement
        throw new UnsupportedOperationException();
    }
}