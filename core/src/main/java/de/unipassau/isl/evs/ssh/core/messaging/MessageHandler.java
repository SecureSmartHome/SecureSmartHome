package de.unipassau.isl.evs.ssh.core.messaging;

public interface MessageHandler {
    void handle(Message.AdressedMessage message);
}
