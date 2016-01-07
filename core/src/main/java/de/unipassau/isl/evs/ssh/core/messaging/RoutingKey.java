package de.unipassau.isl.evs.ssh.core.messaging;

import android.support.annotation.NonNull;

/**
 * @author Niko Fink
 */
public class RoutingKey<T> {
    private static final String SUFFIX_REPLY = "/reply";
    private final Class<T> clazz;
    private final String key;

    public RoutingKey(String key, Class<T> clazz) {
        if (clazz == null) {
            throw new NullPointerException("class");
        }
        if (key == null) {
            throw new NullPointerException("key");
        }
        this.clazz = clazz;
        this.key = key;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> RoutingKey<T> forName(String identifier, String clazz)
            throws ClassNotFoundException, ClassCastException {
        return new RoutingKey<>(identifier, (Class<T>) Class.forName(clazz));
    }

    @NonNull
    public static RoutingKey forMessage(Message.AddressedMessage message) {
        return new RoutingKey<>(message.getRoutingKey(), message.getPayloadUnchecked().getClass());
    }

    @NonNull
    public Class<T> getPayloadClass() {
        return clazz;
    }

    @NonNull
    public String getKey() {
        return key;
    }

    @NonNull
    public <V> RoutingKey<V> getReply(Class<V> replyPayload) {
        return new RoutingKey<>(getReplyKey(key), replyPayload);
    }

    @NonNull
    public static String getReplyKey(String key) {
        return key + SUFFIX_REPLY;
    }

    public boolean isReply() {
        return key.endsWith(SUFFIX_REPLY);
    }

    public boolean matches(Message.AddressedMessage message) {
        return getKey().equals(message.getRoutingKey()) && payloadMatches(message);
    }

    public boolean payloadMatches(Message message) {
        return getPayloadClass().isInstance(message.getPayloadUnchecked()) ||
                (getPayloadClass() == Void.class && message.getPayloadUnchecked() == null);
    }

    public T getPayload(Message message) {
        if (!payloadMatches(message)
                || (message instanceof Message.AddressedMessage && !matches((Message.AddressedMessage) message))) {
            throw new IllegalArgumentException("Message doesn't match RoutingKey " + this);
        }
        return message.getPayloadChecked(getPayloadClass());
    }

    @Override
    public String toString() {
        return getKey() + ":" + getPayloadClass().getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoutingKey key = (RoutingKey) o;
        return clazz.equals(key.clazz) && this.key.equals(key.key);
    }

    @Override
    public int hashCode() {
        return 31 * clazz.hashCode() + key.hashCode();
    }
}
