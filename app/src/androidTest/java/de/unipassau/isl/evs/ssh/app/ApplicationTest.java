package de.unipassau.isl.evs.ssh.app;

import android.test.ApplicationTestCase;

import de.unipassau.isl.evs.ssh.core.container.Container;

public class ApplicationTest extends ApplicationTestCase {
    public ApplicationTest(Class applicationClass) {
        super(applicationClass);
    }

    public void testDependencies() {
        assertTrue("core not available", Container.components.contains("core"));
        assertFalse("drivers available", Container.components.contains("drivers"));
    }
}