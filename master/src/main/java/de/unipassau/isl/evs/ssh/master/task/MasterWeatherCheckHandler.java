package de.unipassau.isl.evs.ssh.master.task;

import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * Task/Handler that periodically checks the records of weather data provider and issues notifications based on a configured set of rules.
 */
public class MasterWeatherCheckHandler implements Task, Handler {
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