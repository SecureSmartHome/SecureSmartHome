package de.unipassau.isl.evs.ssh.core.container;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import de.ncoder.typedmap.Key;
import de.ncoder.typedmap.TypedMap;

/**
 * Container classes deal with instantiation and set up of dependencies for all Components.
 * <p/>
 * Containers are the root element of the systems using the dependency injection design pattern.
 * Containers manage Components and store them in a typed map.
 *
 * @author Niko Fink
 */
public interface Container {
    /**
     * Associates the specified Component with the specified key in the TypedMap of the Container.
     * Also calls {@link Component#init(Container)}.
     *
     * @param key       key with which the specified value is to be associated.
     * @param component Component to be associated with the specified key.
     */
    <T extends Component, V extends T> void register(Key<T> key, V component);

    /**
     * Removes the Component with the specified key from the TypedMap of the Container.
     * Also calls {@link Component#destroy()}.
     *
     * @param key key with which the specified value is to be associated.
     */
    void unregister(Key<?> key);

    /**
     * Removes the Component with all its mapped keys from the TypedMap of the Container.
     * Also calls {@link Component#destroy()}.
     *
     * @param component Component to be removed
     */
    void unregister(Component component);

    /**
     * Returns the Component to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     * This function also makes sure the return type matches the type
     * the Component was initially stored by.
     *
     * @param key The key whose associated value is to be returned.
     * @return the associated Component.
     */
    @Nullable
    <T extends Component> T get(Key<T> key);

    /**
     * Returns the Component to which the specified key is mapped,
     * or throws an {@link IllegalStateException} if this map contains no mapping for the key.
     * This function also makes sure the return type matches the type
     * the Component was initially stored by.
     *
     * @param key The key whose associated value is to be returned.
     * @return the associated Component.
     * @throws IllegalStateException if this map contains no mapping for the key
     */
    @NonNull
    <T extends Component> T require(Key<T> key);

    /**
     * @return {@code true}, if a Component is registered for the given key.
     */
    boolean isRegistered(Key<?> key);

    /**
     * @return an unmodifiable view of all Components contained in this Container.
     */
    @NonNull
    TypedMap<? extends Component> getData();

    /**
     * Remove all Components from this Container.
     *
     * @see #unregister(Component)
     */
    void shutdown();

    class ComponentException extends IllegalStateException {
        private final Key<?> key;

        public ComponentException(Key<?> key, String expected, String actual) {
            super("Illegal Component status for key " + key + "\n" +
                    "expected: " + expected + "\n" +
                    "actual:   " + actual);
            this.key = key;
        }

        public ComponentException(Key<?> key, Component expected, Component actual) {
            this(key, String.valueOf(expected), String.valueOf(actual));
        }

        public ComponentException(Key<?> key, boolean expectedNull) {
            this(key, expectedNull ? "null" : "non-null", !expectedNull ? "null" : "non-null");
        }

        public Key<?> getKey() {
            return key;
        }
    }
}