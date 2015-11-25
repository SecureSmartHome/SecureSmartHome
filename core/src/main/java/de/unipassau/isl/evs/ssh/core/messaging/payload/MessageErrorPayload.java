package de.unipassau.isl.evs.ssh.core.messaging.payload;

/**
 * Payload for messages which indicate initial message
 *
 * @author Chris
 */
public class MessageErrorPayload implements MessagePayload {

    String receiver;
    MessagePayload original;

    public MessageErrorPayload(String receiver, MessagePayload original) {
        this.receiver = receiver;
        this.original = original;
    }

    public String getReceiver() {
        return receiver;
    }

    public MessagePayload getOriginal() {
        return original;
    }
}
