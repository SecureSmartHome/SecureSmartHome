package de.unipassau.isl.evs.ssh.master.task;

import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * Task/Handler that periodically checks the data provided by a weather sensor and issues notifications based on a configured set of rules.
 */
public class MasterHumidityCheckHandler implements Task, Handler {
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