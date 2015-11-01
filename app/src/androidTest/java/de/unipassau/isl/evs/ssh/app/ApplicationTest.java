package de.unipassau.isl.evs.ssh.app;

import android.test.ApplicationTestCase;

import junit.framework.TestCase;

import java.sql.Driver;

import de.unipassau.isl.evs.ssh.core.Container;

public class ApplicationTest extends ApplicationTestCase {
    public ApplicationTest(Class applicationClass) {
        super(applicationClass);
    }

    public void testDependencies() {
        assertTrue("core not available", Container.components.contains("core"));
        assertFalse("drivers available", Container.components.contains("drivers"));
    }
}