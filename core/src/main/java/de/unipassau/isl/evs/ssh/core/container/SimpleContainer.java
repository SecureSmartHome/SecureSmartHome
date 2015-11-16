package de.unipassau.isl.evs.ssh.core.container;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.ncoder.typedmap.Key;
import de.ncoder.typedmap.TypedMap;

public class SimpleContainer implements Container {
    private final TypedMap<Component> components = new TypedMap<>(new ConcurrentHashMap<Key<? extends Component>, Component>());
    private final List<Key<? extends Component>> log = new LinkedList<>();
    private transient TypedMap<Component> componentsUnmodifiable;

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

    @Override
    public <T extends Component> T require(Key<T> key) {
        if (!components.containsKey(key)) {
            throw new ComponentException(key, false);
        }
        return get(key);
    }

    @Override
    public boolean isRegistered(Key<?> key) {
        return components.containsKey(key);
    }

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

    public TypedMap<Component> getData() {
        if (componentsUnmodifiable == null) {
            componentsUnmodifiable = components.unmodifiableView();
        }
        return componentsUnmodifiable;
    }
}