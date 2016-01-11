package de.unipassau.isl.evs.ssh.core.container;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import de.ncoder.typedmap.Key;

/**
 * Components are main parts of the system that have control over their initiation and shutdown process.
 * This means the steps needed to initialize or safely shutdown a Component is managed by itself,
 * whereas the time when either process takes place depends on the Object managing the component.
 * <p/>
 * AbstractComponent provides implementations for standard methods, that will be used multiple times.
 *
 * @author Niko Fink
 */
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

    /**
     * Returns whether this component is active or not
     *
     * @return boolean whether its active
     */
    protected boolean isActive() {
        return container != null;
    }

    /**
     * @return the Container this Component is registered to or {@code null}
     */
    @Nullable
    protected Container getContainer() {
        return container;
    }

    /**
     * Fetch the Component from the Container or return {@code null} if the Component or the Container itself are not available.
     *
     * @see Container#get(Key)
     */
    @Nullable
    protected <T extends Component> T getComponent(Key<T> key) {
        Container container = getContainer();
        if (container != null) {
            return container.get(key);
        } else {
            return null;
        }
    }

    /**
     * Fetch the Component from the Container or throw an {@link IllegalStateException} if the Component or the
     * Container itself are not available.
     *
     * @see Container#require(Key)
     */
    @NonNull
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