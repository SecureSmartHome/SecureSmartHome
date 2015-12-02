package de.unipassau.isl.evs.ssh.core.container;

import android.util.Log;

import de.ncoder.typedmap.Key;

/**
 * Components are main parts of the system that have control over their initiation and shutdown process.
 * This means the steps needed to initialize or safely shutdown a Component is managed by itself,
 * whereas the time when either process takes place depends on the Object managing the component.
 *
 * AbstractComponent provides implementations for standard methods, that will be used multiple times.
 *
 * @author Niko
 */
public class AbstractComponent implements Component {
    private Container container;

    /**
     * @inheritDoc
     */
    @Override
    public void init(Container container) {
        this.container = container;
        Log.v(getClass().getSimpleName(), "init");
    }

    /**
     * @inheritDoc
     */
    @Override
    public void destroy() {
        container = null;
        Log.v(getClass().getSimpleName(), "destroy");
    }

    /**
     * Returns whether this component is active or not
     *
     * @return boolean whether its active
     */
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

    /**
     * Checks and returns whether a component required by the given component is
     * already registered in the container.
     *
     * @param key of the component thats needed
     * @param <T> type of the component thats needed
     *
     * @return component thats needed
     */
    protected <T extends Component> T requireComponent(Key<T> key) {
        Container container = getContainer();
        if (container != null) {
            return container.require(key);
        } else {
            throw new IllegalStateException("Component not registered to a container");
        }
    }

    /**
     * Method that returns a String for the given component
     * @return String for the given component
     */
    @Override
    public String toString() {
        String name = getClass().getSimpleName();
        if (name.isEmpty()) {
            name = getClass().getName();
            int index = name.lastIndexOf(".");
            if (index >= 0 && index + 1 < name.length()) {
                name = name.substring(index + 1);
            }
        }
        return name;
    }
}