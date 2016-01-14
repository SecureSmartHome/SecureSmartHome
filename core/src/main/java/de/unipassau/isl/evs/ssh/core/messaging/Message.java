package de.unipassau.isl.evs.ssh.core.messaging;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import de.ncoder.typedmap.Key;
import de.ncoder.typedmap.TypedMap;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import io.netty.util.concurrent.Future;

/**
 * Message are used to exchange information between devices and handlers.
 * A Message contains a header with information about the Message itself and
 * a payload which contains to information for the intended device and handler.
 *
 * @author Niko Fink
 */
public class Message implements Serializable {
    public static final Key<Long> HEADER_TIMESTAMP = new Key<>(Long.class, "timestamp");
    public static final Key<Integer> HEADER_REFERENCES_ID = new Key<>(Integer.class, "referencesID");
    /**
     * @deprecated replaced by {@link RoutingKey#getReplyKey(String)}
     */
    @Deprecated
    public static final Key<String> HEADER_REPLY_TO_KEY = new Key<>(String.class, "replyToKey");

    @NonNull
    private final TypedMap<Object> headers;
    @Nullable
    private MessagePayload payload;

    public Message() {
        this(null);
    }

    public Message(@Nullable MessagePayload payload) {
        this(new TypedMap<>(), payload);
    }

    @SuppressWarnings("unchecked")
    private Message(@NonNull TypedMap headers, @Nullable MessagePayload payload) {
        this.headers = headers;
        this.payload = payload;
    }

    /**
     * @deprecated use {@link RoutingKey#getPayload(Message)} for type-checked access that also checks the RoutingKey,
     * or at least {@link #getPayloadChecked(Class)} for type-save access when the same Payload class is used for multiple RoutingKeys.
     */
    @Nullable
    @Deprecated
    public MessagePayload getPayload() {
        return getPayloadUnchecked();
    }

    @Nullable
    MessagePayload getPayloadUnchecked() {
        return payload;
    }

    /**
     * Cast and return the payload contained in this Message to the given class.
     * If the one single RoutingKey is known for this message, use {@link RoutingKey#getPayload(Message)} instead.
     *
     * @throws ClassCastException
     */
    @Nullable
    public <T> T getPayloadChecked(Class<T> payloadClass) {
        return payloadClass.cast(getPayloadUnchecked());
    }

    public void setPayload(@Nullable MessagePayload payload) {
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

    @NonNull
    public TypedMap<Object> getHeaders() {
        return headers;
    }

    @Override
    public String toString() {
        StringBuilder bob = new StringBuilder();
        bob.append("<").append(headerString()).append(">\n");

        //Headers
        for (Map.Entry<Key<?>, Object> entry : headers.entrySet()) {
            bob.append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append("\n");
        }

        //Payload
        if (payload == null) {
            bob.append("null");
        } else if (payload instanceof Throwable) {
            bob.append(Log.getStackTraceString((Throwable) payload));
        } else {
            payloadToString(bob);
        }

        return bob.toString();
    }

    private void payloadToString(StringBuilder bob) {
        assert payload != null;
        bob.append(payload.getClass().getName()).append("{\n");
        for (Field field : getFields(payload.getClass())) {
            Object value;
            try {
                final boolean wasAccessible = field.isAccessible();
                field.setAccessible(true);
                value = field.get(payload);
                if (!wasAccessible) {
                    field.setAccessible(false);
                }
            } catch (IllegalAccessException e) {
                value = e;
            }
            String string = valueToString(value);
            if (string.length() > 1000) {
                string = string.substring(0, 997) + "...";
            }
            bob.append("\t").append(field.getName()).append("=").append(string).append("\n");
        }
        bob.append("}");
    }

    @NonNull
    private String valueToString(Object value) {
        if (value == null) {
            return String.valueOf((Object) null);
        } else if (value instanceof Throwable) {
            return Log.getStackTraceString((Throwable) value);
        } else if (value instanceof boolean[]) {
            return Arrays.toString((boolean[]) value);
        } else if (value instanceof byte[]) {
            return Arrays.toString((byte[]) value);
        } else if (value instanceof char[]) {
            return Arrays.toString((char[]) value);
        } else if (value instanceof double[]) {
            return Arrays.toString((double[]) value);
        } else if (value instanceof float[]) {
            return Arrays.toString((float[]) value);
        } else if (value instanceof int[]) {
            return Arrays.toString((int[]) value);
        } else if (value instanceof long[]) {
            return Arrays.toString((long[]) value);
        } else if (value instanceof short[]) {
            return Arrays.toString((short[]) value);
        } else if (value instanceof Object[]) {
            return Arrays.deepToString((Object[]) value);
        } else {
            return String.valueOf(value);
        }
    }

    @NonNull
    private List<Field> getFields(Class<? extends MessagePayload> clazz) {
        Set<Field> fieldSet = new HashSet<>();
        fieldSet.addAll(Arrays.asList(clazz.getFields()));
        for (Class c = clazz; c != Object.class && c != null; c = c.getSuperclass()) {
            fieldSet.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        List<Field> fieldList = new ArrayList<>();
        fieldList.addAll(fieldSet);
        Collections.sort(fieldList, new Comparator<Field>() {
            @Override
            public int compare(Field lhs, Field rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });
        return fieldList;
    }

    protected CharSequence headerString() {
        return getClass().getSimpleName();
    }

    /**
     * Extend this Message to an AddressedMessage, also sets {@link #HEADER_TIMESTAMP} and
     * {@link AddressedMessage#getSequenceNr() sequence number}.
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
     * An AddressedMessage is an immutable Message with additional information about sender and receiver.
     * Additionally, the String part of the RoutingKey is transferred. The Class part will be inferred based on the used payload.
     */
    public static class AddressedMessage extends Message {
        private final DeviceID fromID;
        private final DeviceID toID;
        private final String routingKey;
        private final int sequenceNr;

        private transient Future<Void> sendFuture;

        private static final AtomicInteger sequenceCounter = new AtomicInteger();

        private AddressedMessage(Message from, DeviceID fromID, DeviceID toID, String routingKey) {
            this(new TypedMap<>(from.headers), from.payload, fromID, toID, routingKey);
        }

        private AddressedMessage(TypedMap headers, @Nullable MessagePayload payload, DeviceID fromID, DeviceID toID, String routingKey) {
            super(headers.unmodifiableView(), payload);
            if (fromID == null) {
                throw new NullPointerException("fromID");
            }
            if (toID == null) {
                throw new NullPointerException("toID");
            }
            if (routingKey == null) {
                throw new NullPointerException("routingKey");
            }
            this.fromID = fromID;
            this.toID = toID;
            this.routingKey = routingKey;
            sequenceNr = sequenceCounter.getAndIncrement();
        }

        /**
         * @throws UnsupportedOperationException this class is immutable
         */
        @Override
        public void setPayload(MessagePayload payload) {
            throw new UnsupportedOperationException();
        }

        /**
         * @throws UnsupportedOperationException this class is immutable
         */
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

        /**
         * @return a Future indicating if this message was sent successfully or not. {@code null} for received AddressedMessages.
         */
        public Future<Void> getSendFuture() {
            return sendFuture;
        }

        void setSendFuture(Future<Void> sendFuture) {
            this.sendFuture = sendFuture;
        }

        @Override
        protected CharSequence headerString() {
            return super.headerString() + "#" + sequenceNr + " from " + fromID.toShortString() +" to " + toID.toShortString() + routingKey;
        }
    }
}
