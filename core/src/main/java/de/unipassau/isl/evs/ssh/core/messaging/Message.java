package de.unipassau.isl.evs.ssh.core.messaging;

import java.io.Serializable;

import de.ncoder.typedmap.Key;
import de.ncoder.typedmap.TypedMap;
import de.unipassau.isl.evs.ssh.core.naming.OdroidID;

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

    public <T> T getHeader(Key<T> key) {
        return headers.get(key);
    }

    public <T> T putHeader(Key<T> key, T value) {
        return headers.putTyped(key, value);
    }

    public <T> T removeHeader(Key<T> key) {
        return headers.remove(key);
    }

    public TypedMap<Object> getHeaders() {
        return headers;
    }

    public AdressedMessage setDestination(OdroidID fromID, OdroidID toID, String toHandler) {
        return new AdressedMessage(this, fromID, toID, toHandler);
    }

    public static class AdressedMessage extends Message {
        public OdroidID fromID;
        public OdroidID toID;
        public String routingKey;

        private AdressedMessage(Message from, OdroidID fromID, OdroidID toID, String routingKey) {
            this(new TypedMap<>(from.headers), from.payload, fromID, toID, routingKey);
        }

        public AdressedMessage(TypedMap headers, MessagePayload payload, OdroidID fromID, OdroidID toID, String routingKey) {
            super(headers, payload);
            this.fromID = fromID;
            this.toID = toID;
            this.routingKey = routingKey;
        }

        @Override
        public void setPayload(MessagePayload payload) {
            throw new UnsupportedOperationException();
        }

        public OdroidID getFromID() {
            return fromID;
        }

        public OdroidID getToID() {
            return toID;
        }

        public String getRoutingKey() {
            return routingKey;
        }
    }
}
