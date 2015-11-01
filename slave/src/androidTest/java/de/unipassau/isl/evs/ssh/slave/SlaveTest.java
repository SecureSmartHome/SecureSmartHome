package de.unipassau.isl.evs.ssh.slave;

import android.test.ApplicationTestCase;

import de.unipassau.isl.evs.ssh.core.Container;
import de.unipassau.isl.evs.ssh.drivers.Driver;

public class SlaveTest extends ApplicationTestCase {
    public SlaveTest(Class applicationClass) {
        super(applicationClass);
    }

    public void testDependencies() {
        Driver.load();
        assertTrue("core not available", Container.components.contains("core"));
        assertTrue("drivers not available", Container.components.contains("drivers"));
    }
}