package de.unipassau.isl.evs.ssh.core.container;

class TestComponent extends AbstractComponent {
    int initCount;
    int destroyCount;

    @Override
    public void init(Container container) {
        super.init(container);
        initCount++;
    }

    @Override
    public void destroy() {
        super.destroy();
        destroyCount++;
    }
}
