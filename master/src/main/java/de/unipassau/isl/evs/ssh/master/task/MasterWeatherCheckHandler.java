package de.unipassau.isl.evs.ssh.master.task;

import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * Task/Handler that periodically checks the records of weather data provider and issues notifications based on a configured set of rules.
 */
public class MasterWeatherCheckHandler implements Task, MessageHandler {

    @Override
    public void run() {
        //TODO implement
        throw new UnsupportedOperationException();
    }

    @Override
    public void handle(Message.AddressedMessage message) {

    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {

    }
}