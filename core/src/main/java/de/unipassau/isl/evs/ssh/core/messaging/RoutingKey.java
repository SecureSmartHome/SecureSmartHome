package de.unipassau.isl.evs.ssh.core.messaging;

/**
 * TODO also differentiate between request and response, i.e. for message.getHeader(Message.HEADER_REFERENCES_ID) == null (Niko, 2015-12-17)
 *
 * @author Niko Fink
 */
public class RoutingKey<T> {
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

    @SuppressWarnings("unchecked")
    public static <T> RoutingKey<T> forName(String identifier, String clazz)
            throws ClassNotFoundException, ClassCastException {
        return new RoutingKey<>(identifier, (Class<T>) Class.forName(clazz));
    }

    public static RoutingKey forMessage(Message.AddressedMessage message) {
        return new RoutingKey<>(message.getRoutingKey(), message.getPayloadUnchecked().getClass());
    }

    public Class<T> getPayloadClass() {
        return clazz;
    }

    public String getKey() {
        return key;
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
