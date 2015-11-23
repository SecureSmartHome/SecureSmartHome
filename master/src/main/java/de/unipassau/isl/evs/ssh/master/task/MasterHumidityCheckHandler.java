package de.unipassau.isl.evs.ssh.master.task;

import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * Task/Handler that periodically checks the data provided by a weather sensor and issues notifications based on a configured set of rules.
 */
public class MasterHumidityCheckHandler implements Task, MessageHandler {

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