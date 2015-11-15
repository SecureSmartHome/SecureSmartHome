package de.unipassau.isl.evs.ssh.master.task;

import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * Task/Handler that periodically checks the brightness in the room, the associated sensor is in,
 * and issues notifications based on a configured set of rules.
 * <p/>
 * An example for this would be a Notification that suggests to turn off the light if it is too bright.
 */
public class MasterBrightnessCheckHandler implements Task, Handler {
    @Override
    public void handle(Message message) {
        //TODO implement
        throw new UnsupportedOperationException();
    }

    @Override
    public void run() {
        //TODO implement
        throw new UnsupportedOperationException();
    }
}