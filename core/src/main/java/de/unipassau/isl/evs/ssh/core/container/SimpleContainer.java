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
import android.util.Log;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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
public class SimpleContainer implements Container {
    private final String TAG = "SContainer@" + Objects.hashCode(this);

    private final TypedMap<Component> components = new TypedMap<>(new ConcurrentHashMap<Key<? extends Component>, Component>());
    private final List<Key<? extends Component>> log = new LinkedList<>();
    private transient TypedMap<Component> componentsUnmodifiable;

    public SimpleContainer() {
        Log.v(TAG, "constructor");
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Also records the order in which the Components were added, so that they can be removed in shutdown without
     * violating any dependency constraints.
     */
    @Override
    public synchronized <T extends Component, V extends T> void register(Key<T> key, V component) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (component == null) {
            throw new NullPointerException("component");
        }
        if (isRegistered(key)) {
            throw new ComponentException(key, null, get(key));
        }
        log.add(key);
        components.putTyped(key, component);
        component.init(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void unregister(Key<?> keyUnchecked) {
        if (Component.class.isAssignableFrom(keyUnchecked.getValueClass())) {
            Key<? extends Component> key = (Key<? extends Component>) keyUnchecked;
            Component component = components.remove(key);
            if (component != null) {
                component.destroy();
            }
        } else {
            components.remove(keyUnchecked);
        }
    }

    @Override
    public synchronized void unregister(Component component) {
        boolean removed = false;
        Iterator<Map.Entry<Key<? extends Component>, Component>> it = components.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Key<? extends Component>, Component> entry = it.next();
            if (entry.getValue() == component) {
                it.remove();
                removed = true;
            }
        }
        if (removed) {
            component.destroy();
        }
    }

    @Override
    public <T extends Component> T get(Key<T> key) {
        return components.get(key);
    }

    @NonNull
    @Override
    public <T extends Component> T require(Key<T> key) {
        final T t = get(key);
        if (t == null) { //for HashMap, get(key) has same processing time as containsKey(key)
            throw new ComponentException(key, false);
        }
        return t;
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public boolean isRegistered(Key<?> key) {
        return components.containsKey(key);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Removes the Components in the order they were added, so that no dependency constraints are violated.
     */
    @Override
    public void shutdown() {
        Log.v(TAG, "shutdown:called");
        ListIterator<Key<? extends Component>> it = log.listIterator(log.size());
        while (it.hasPrevious()) {
            Key<? extends Component> key = it.previous();
            Component component = components.remove(key);
            if (component != null) {
                component.destroy();
            }
        }
        if (!components.isEmpty()) Log.wtf(TAG, "shutdown: not all components were removed: " + components);
        Log.d(TAG, "shutdown:finished");
    }

    @NonNull
    public TypedMap<Component> getData() {
        if (componentsUnmodifiable == null) {
            componentsUnmodifiable = components.unmodifiableView();
        }
        return componentsUnmodifiable;
    }
}