package de.unipassau.isl.evs.ssh.app;

import android.test.ActivityInstrumentationTestCase2;

import de.unipassau.isl.evs.ssh.app.activity.MainActivity;
import de.unipassau.isl.evs.ssh.core.container.Container;

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    public MainActivityTest() {
        super(MainActivity.class);
    }

    public void testDependencies() {
        //Driver.load();
        assertTrue("core not available", Container.components.contains("core"));
        assertFalse("drivers available", Container.components.contains("drivers"));
    }
}