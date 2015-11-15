package de.unipassau.isl.evs.ssh.core.container;

/**
 * Components are main parts of the system that have control over their initiation and shutdown process.
 * This means the steps needed to initialize or safely shutdown a Component is managed by itself,
 * whereas the time when either process takes place depends on the Object managing the component.
 */
public interface Component {

    /**
     * This functions allows the Container to initialize a class implementing the Component interface.
     *
     * @param container The Container calling this function will reference itself so that Components
     *                  know by whom they are managed.
     */
    void init(Container container);

    /**
     * This function will be called by the Container to signalize a class implementing the Component
     * interface that it will no longer be managed by the Container.
     */
    void destroy();

}