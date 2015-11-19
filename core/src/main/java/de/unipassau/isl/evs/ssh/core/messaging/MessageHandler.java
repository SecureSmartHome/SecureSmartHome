package de.unipassau.isl.evs.ssh.core.messaging;

public interface MessageHandler {
    void handlerAdded(de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher dispatcher, String routingKey);

    void handlerRemoved(String routingKey);

    void handle(Message.AddressedMessage message);
}
