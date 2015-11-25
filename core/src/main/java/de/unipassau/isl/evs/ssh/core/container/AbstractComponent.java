package de.unipassau.isl.evs.ssh.core.container;

import android.util.Log;

import de.ncoder.typedmap.Key;

public class AbstractComponent implements Component {
    private Container container;

    @Override
    public void init(Container container) {
        this.container = container;
        Log.v(getClass().getSimpleName(), "init");
    }

    @Override
    public void destroy() {
        container = null;
        Log.v(getClass().getSimpleName(), "destroy");
    }

    protected boolean isActive() {
        return container != null;
    }

    protected Container getContainer() {
        return container;
    }

    protected <T extends Component> T getComponent(Key<T> key) {
        Container container = getContainer();
        if (container != null) {
            return container.get(key);
        } else {
            return null;
        }
    }

    protected <T extends Component> T requireComponent(Key<T> key) {
        Container container = getContainer();
        if (container != null) {
            return container.require(key);
        } else {
            throw new IllegalStateException("Component not registered to a container");
        }
    }

    @Override
    public String toString() {
        String name = getClass().getSimpleName();
        if (name == null || name.isEmpty()) {
            name = getClass().getName();
            int index = name.lastIndexOf(".");
            if (index >= 0 && index + 1 < name.length()) {
                name = name.substring(index + 1);
            }
        }
        return name;
    }
}