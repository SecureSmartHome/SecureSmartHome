package de.unipassau.isl.evs.ssh.core.messaging;

/**
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

    public Class<T> getPayloadClass() {
        return clazz;
    }

    public String getKey() {
        return key;
    }

    public boolean matches(Message.AddressedMessage message) {
        return getKey().equals(message.getRoutingKey()) && getPayloadClass().isInstance(message.getPayload());
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
