package de.unipassau.isl.evs.ssh.core.container;

import android.content.Intent;
import android.test.ServiceTestCase;

import de.ncoder.typedmap.Key;

public class ContainerServiceTest extends ServiceTestCase<ContainerService> {
    private Key<TestComponent> k = new Key<>(TestComponent.class);
    private TestComponent c = new TestComponent();

    public ContainerServiceTest() {
        super(ContainerService.class);
    }

    public void test() {
        ContainerService.Binder binder = (ContainerService.Binder) bindService(new Intent(getContext(), ContainerService.class));

        assertNotNull(binder);
        assertNotNull(binder.getData());

        assertFalse(c.isActive());
        assertNull(binder.get(k));

        binder.register(k, c);

        assertTrue(c.isActive());
        assertNotNull(binder.get(k));

        shutdownService();

        assertFalse(c.isActive());
        assertEquals(c.destroyCount, 1);
    }
}