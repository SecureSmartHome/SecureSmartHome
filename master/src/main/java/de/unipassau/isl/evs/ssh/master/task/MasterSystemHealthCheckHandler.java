package de.unipassau.isl.evs.ssh.master.task;

import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * Task/Handler that periodically checks if hardware system components are still active and working properly.
 * In case of malfunctioning hardware notifications are issued.
 */
public class MasterSystemHealthCheckHandler implements Task, MessageHandler {

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