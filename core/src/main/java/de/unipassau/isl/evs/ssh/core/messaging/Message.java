package de.unipassau.isl.evs.ssh.core.messaging;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import de.ncoder.typedmap.Key;
import de.ncoder.typedmap.TypedMap;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import io.netty.channel.ChannelFuture;

/**
 * Message are used to exchange information between devices and handlers.
 * A Message contains a header with information about the Message itself and
 * a payload which contains to information for the intended device and handler.
 * @author Niko
 */
public class Message implements Serializable {
    public static final Key<Long> HEADER_TIMESTAMP = new Key<>(Long.class, "timestamp");
    public static final Key<Integer> HEADER_REFERENCES_ID = new Key<>(Integer.class, "referencesID");
    public static final Key<String> HEADER_REPLY_TO_KEY = new Key<>(String.class, "replyToKey");

    private static final AtomicInteger sequenceCounter = new AtomicInteger();
    private final TypedMap<Object> headers;
    private MessagePayload payload;

    public Message() {
        this(null);
    }

    public Message(MessagePayload payload) {
        this(new TypedMap<>(), payload);
    }

    @SuppressWarnings("unchecked")
    private Message(TypedMap headers, MessagePayload payload) {
        this.headers = headers;
        this.payload = payload;
    }

    public MessagePayload getPayload() {
        return payload;
    }

    public void setPayload(MessagePayload payload) {
        this.payload = payload;
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

    @Override
    public String toString() {
        StringBuilder bob = new StringBuilder();
        bob.append("<").append(headerString()).append(">\n");
        for (Map.Entry<Key<?>, Object> entry : headers.entrySet()) {
            bob.append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append("\n");
        }
        bob.append(payload)
                .append("\n");
        return bob.toString();
    }

    protected CharSequence headerString() {
        return getClass().getSimpleName();
    }

    /**
     * Extend this Message to an AddressedMessage.
     *
     * @param fromID     ID of the sending device.
     * @param toID       ID of the receiving device.
     * @param routingKey Alias of the receiving Handler.
     */
    AddressedMessage setDestination(DeviceID fromID, DeviceID toID, String routingKey) {
        this.putHeader(HEADER_TIMESTAMP, System.currentTimeMillis());
        return new AddressedMessage(this, fromID, toID, routingKey);
    }

    /**
     * An AddressedMessage is a Message with additional information about to sender and the receiver.
     */
    public static class AddressedMessage extends Message {
        private final DeviceID fromID;
        private final DeviceID toID;
        private final String routingKey;
        private final int sequenceNr;

        private transient ChannelFuture sendFuture;

        private AddressedMessage(Message from, DeviceID fromID, DeviceID toID, String routingKey) {
            this(new TypedMap<>(from.headers), from.payload, fromID, toID, routingKey);
        }

        private AddressedMessage(TypedMap headers, MessagePayload payload, DeviceID fromID, DeviceID toID, String routingKey) {
            super(headers.unmodifiableView(), payload);
            this.fromID = fromID;
            this.toID = toID;
            this.routingKey = routingKey;
            sequenceNr = sequenceCounter.getAndIncrement();
        }

        @Override
        public void setPayload(MessagePayload payload) {
            throw new UnsupportedOperationException();
        }

        @Override
        AddressedMessage setDestination(DeviceID fromID, DeviceID toID, String routingKey) {
            throw new UnsupportedOperationException("destination already set");
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

        public int getSequenceNr() {
            return sequenceNr;
        }

        public ChannelFuture getSendFuture() {
            return sendFuture;
        }

        void setSendFuture(ChannelFuture sendFuture) {
            this.sendFuture = sendFuture;
        }

        @Override
        protected CharSequence headerString() {
            return super.headerString() + "#" + sequenceNr + " to " + toID
                    + (routingKey != null && routingKey.startsWith("/") ? "/" : "") + routingKey + " from " + fromID;
        }
    }
}
