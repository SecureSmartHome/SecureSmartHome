package de.unipassau.isl.evs.ssh.core.container;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.ncoder.typedmap.Key;
import de.ncoder.typedmap.TypedMap;

public class SimpleContainer implements Container {
    private final TypedMap<Component> components = new TypedMap<>(new ConcurrentHashMap<Key<? extends Component>, Component>());
    private final List<Key<? extends Component>> log = new LinkedList<>();

    @Override
    public synchronized <T extends Component, V extends T> void register(Key<T> key, V component) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (component == null) {
            throw new NullPointerException("component");
        }
        if (isRegistered(key)) {
            throw new IllegalArgumentException("Component for " + key + " already registered");
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

    @Override
    public <T extends Component> T require(Key<T> key) {
        if (!components.containsKey(key)) {
            throw new IllegalStateException("No Component registered for Key " + key);
        }
        return get(key);
    }

    @Override
    public boolean isRegistered(Key<?> key) {
        return components.containsKey(key);
    }

    private transient TypedMap<Component> componentsUnmodifiable;

    public TypedMap<Component> getData() {
        if (componentsUnmodifiable == null) {
            componentsUnmodifiable = components.unmodifiableView();
        }
        return componentsUnmodifiable;
    }

    private transient UnmodifiableSet<Key<? extends Component>> keysUnmodifiable;

    @Override
    public Collection<Key<? extends Component>> getKeys() {
        if (keysUnmodifiable == null) {
            keysUnmodifiable = new UnmodifiableSet<>(components.keySet());
        }
        return keysUnmodifiable;
    }

    // ------------------------------------------------------------------------

    @Override
    public void shutdown() {
        ListIterator<Key<? extends Component>> it = log.listIterator(log.size());
        while (it.hasPrevious()) {
            Key<? extends Component> key = it.previous();
            Component component = components.remove(key);
            if (component != null) {
                component.destroy();
            }
        }
        assert components.isEmpty() : "Not all components were removed: " + components;
    }

    // ------------------------------------------------------------------------

    private static class UnmodifiableSet<E> implements Set<E>, Serializable {
        private transient Set<E> s;

        private UnmodifiableSet(Set<E> collection) {
            s = collection;
        }

        @Override
        public boolean add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(Object o) {
            return s.contains(o);
        }

        @Override
        public boolean containsAll(Collection<?> o) {
            return s.containsAll(o);
        }

        @Override
        public boolean isEmpty() {
            return s.isEmpty();
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {
                Iterator<E> iterator = s.iterator();

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public E next() {
                    return iterator.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return s.size();
        }

        @Override
        public Object[] toArray() {
            return s.toArray();
        }

        @Override
        public <T> T[] toArray(T[] array) {
            return s.toArray(array);
        }

        @Override
        public String toString() {
            return s.toString();
        }

        @Override
        public boolean equals(Object object) {
            return s.equals(object);
        }

        @Override
        public int hashCode() {
            return s.hashCode();
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            stream.defaultWriteObject();
            stream.writeInt(s.size());
            for (E key : s) {
                stream.writeObject(key);
            }
        }

        @SuppressWarnings("unchecked")
        private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
            stream.defaultReadObject();
            int size = stream.readInt();
            s = new HashSet<>(size);
            for (int i = size; --i >= 0; ) {
                s.add((E) stream.readObject());
            }
        }
    }
}