package de.unipassau.isl.evs.ssh.core.container;

public interface Component {
    void init(Container container);

    void destroy();
}