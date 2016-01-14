package de.unipassau.isl.evs.ssh.core.container;

import junit.framework.TestCase;

import de.ncoder.typedmap.Key;

public class SimpleContainerTest extends TestCase {
    private final Key<TestComponent> k = new Key<>(TestComponent.class);
    private final Container container = new SimpleContainer();
    private final Key<TestComponent> k1 = new Key<>(TestComponent.class, "1");
    private final Key<TestComponent> k2 = new Key<>(TestComponent.class, "2");
    private final TestComponent c1 = new TestComponent();
    private final TestComponent c2 = new TestComponent();

    public void testRegister() {
        assertFalse(c1.isActive());
        assertFalse(c2.isActive());
        assertNull(container.get(k1));
        assertNull(container.get(k2));
        assertNull(container.get(k));

        container.register(k1, c1);

        assertTrue(c1.isActive());
        assertFalse(c2.isActive());
        assertEquals(container.get(k1), c1);
        assertNull(container.get(k2));
        assertNull(container.get(k));

        container.register(k2, c2);

        assertTrue(c1.isActive());
        assertTrue(c2.isActive());
        assertEquals(container.get(k1), c1);
        assertEquals(container.get(k2), c2);
        assertEquals(container.require(k1), c1);
        assertEquals(container.require(k2), c2);
        assertNull(container.get(k));

        try {
            container.register(k1, c2);
            fail("registered a component for an already registered key");
        } catch (Container.ComponentException e) {
        }

        assertEquals(c1.initCount, 1);
        assertEquals(c2.initCount, 1);

        container.unregister(k1);

        assertFalse(c1.isActive());
        assertTrue(c2.isActive());
        assertNull(container.get(k1));
        assertEquals(container.get(k2), c2);
        assertEquals(c1.destroyCount, 1);
        assertEquals(c2.destroyCount, 0);

        container.unregister(c2);

        assertFalse(c1.isActive());
        assertFalse(c2.isActive());
        assertNull(container.get(k1));
        assertNull(container.get(k2));
        assertEquals(c1.destroyCount, 1);
        assertEquals(c2.destroyCount, 1);

        try {
            container.require(k1);
            fail("missing required dependency");
        } catch (Container.ComponentException e) {
        }
        try {
            container.require(k2);
            fail("missing required dependency");
        } catch (Container.ComponentException e) {
        }
    }

    public void testShutdown() {
        container.register(k1, c1);
        container.register(k2, c2);

        assertTrue(c1.isActive());
        assertTrue(c2.isActive());

        container.shutdown();

        assertFalse(c1.isActive());
        assertFalse(c2.isActive());
        assertNull(container.get(k1));
        assertNull(container.get(k2));
        assertEquals(c1.destroyCount, 1);
        assertEquals(c2.destroyCount, 1);
    }
}