package de.unipassau.isl.evs.ssh.master.task;

import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * HolidayTask that switches on a lamp.
 */
public class LightOn implements HolidayTask, MessageHandler {

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