package de.unipassau.isl.evs.ssh.core.messaging.payload;

/**
 * Payload for messages which indicate initial message
 *
 * @author Christoph Fraedrich
 */
public class MessageErrorPayload implements MessagePayload {

    MessagePayload original;

    public MessageErrorPayload(MessagePayload original) {
        this.original = original;
    }

    public MessagePayload getOriginal() {
        return original;
    }
}
