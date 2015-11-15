package de.unipassau.isl.evs.ssh.master.task;

import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * Task/Handler that periodically checks if hardware system components are still active and working properly.
 * In case of malfunctioning hardware notifications are issued.
 */
public class MasterSystemHealthCheckHandler implements Task, Handler {
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