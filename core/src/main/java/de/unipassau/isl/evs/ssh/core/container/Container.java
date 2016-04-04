/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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

        public ComponentException(Key<?> key, @Nullable Component expected, @Nullable Component actual) {
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