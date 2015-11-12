package de.unipassau.isl.evs.ssh.master;

import android.test.ApplicationTestCase;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.drivers.Driver;

public class MasterTest extends ApplicationTestCase {
    public MasterTest(Class applicationClass) {
        super(applicationClass);
    }

    public void testDependencies() {
        Driver.load();
        assertTrue("core not available", Container.components.contains("core"));
        assertTrue("drivers not available", Container.components.contains("drivers"));
    }
}