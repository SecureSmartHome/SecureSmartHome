package de.unipassau.isl.evs.ssh.core.messaging;

import java.io.Serializable;

import de.ncoder.typedmap.Key;
import de.ncoder.typedmap.TypedMap;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * Message are used to exchange information between devices and handlers.
 * A Message contains a header with information about the Message itself and
 * a playload which contains to information for the intended device and handler.
 */
public class Message implements Serializable {
    public static final Key<Long> HEADER_TIMESTAMP = new Key<>(Long.class, "timestamp");
    public static final Key<Integer> HEADER_MESSAGE_ID = new Key<>(Integer.class, "messageID");
    public static final Key<Integer> HEADER_REFERENCES_ID = new Key<>(Integer.class, "referencesID");
    public static final Key<String> HEADER_REPLY_TO_KEY = new Key<>(String.class, "replyToKey");

    private final TypedMap<Object> headers;
    private MessagePayload payload;

    private Message(TypedMap headers, MessagePayload payload) {
        this.headers = headers;
        this.payload = payload;
    }

    public void setPayload(MessagePayload payload) {
        this.payload = payload;
    }

    public MessagePayload getPayload() {
        return payload;
    }

    /**
     * Get single header information of this Message.
     *
     * @param key Key associated with the desired header information.
     */
    public <T> T getHeader(Key<T> key) {
        return headers.get(key);
    }

    /**
     * Add or update single header information.
     *
     * @param key   Key associated with the header information.
     * @param value New header information.
     */
    public <T> T putHeader(Key<T> key, T value) {
        return headers.putTyped(key, value);
    }

    /**
     * Remove single header information.
     *
     * @param key Key associated with the header information.
     */
    public <T> T removeHeader(Key<T> key) {
        return headers.remove(key);
    }

    public TypedMap<Object> getHeaders() {
        return headers;
    }

    /**
     * Extend this Message to an AddressedMessage.
     *
     * @param fromID     ID of the sending device.
     * @param toID       ID of the receiving device.
     * @param routingKey Alias of the receiving Handler.
     */
    public AddressedMessage setDestination(DeviceID fromID, DeviceID toID, String routingKey) {
        return new AddressedMessage(this, fromID, toID, routingKey);
    }

    /**
     * An AddressedMessage is a Message with additional information about to sender and the receiver.
     */
    public static class AddressedMessage extends Message {
        private DeviceID fromID;
        private DeviceID toID;
        private String routingKey;

        private AddressedMessage(Message from, DeviceID fromID, DeviceID toID, String routingKey) {
            this(new TypedMap<>(from.headers).unmodifiableView(), from.payload, fromID, toID, routingKey);
        }

        private AddressedMessage(TypedMap headers, MessagePayload payload, DeviceID fromID, DeviceID toID, String routingKey) {
            super(headers, payload);
            this.fromID = fromID;
            this.toID = toID;
            this.routingKey = routingKey;
        }

        @Override
        public void setPayload(MessagePayload payload) {
            throw new UnsupportedOperationException();
        }

        public DeviceID getFromID() {
            return fromID;
        }

        public DeviceID getToID() {
            return toID;
        }

        public String getRoutingKey() {
            return routingKey;
        }
    }
}
