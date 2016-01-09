package de.unipassau.isl.evs.ssh.core.messaging.payload;

/**
 * Payload for messages which indicate initial message
 *
 * @author Christoph Fraedrich
 * @deprecated use {@link ErrorPayload} instead, this will never be sent
 */
@Deprecated
public class MessageErrorPayload implements MessagePayload {

    MessagePayload original;

    public MessageErrorPayload(MessagePayload original) {
        this.original = original;
    }

    public MessagePayload getOriginal() {
        return original;
    }
}
