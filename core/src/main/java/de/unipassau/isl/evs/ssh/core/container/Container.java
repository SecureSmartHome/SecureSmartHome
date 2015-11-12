package de.unipassau.isl.evs.ssh.core.container;

import java.util.Arrays;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Set;

import de.ncoder.typedmap.Key;
import de.ncoder.typedmap.TypedMap;

public interface Container {
    Set<String> components = new HashSet<>(Arrays.asList(new String[]{"core"}));

    <T extends Component, V extends T> void register(Key<T> key, V component);

    void unregister(Key<?> key);

    void unregister(Component component);

    <T extends Component> T get(Key<T> key);

    <T extends Component> T require(Key<T> key);

    boolean isRegistered(Key<?> key);

    TypedMap<? extends Component> getData();

    Collection<Key<? extends Component>> getKeys();

    void shutdown();
}