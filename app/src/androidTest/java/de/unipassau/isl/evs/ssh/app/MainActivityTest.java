package de.unipassau.isl.evs.ssh.app;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.TextView;

import de.unipassau.isl.evs.ssh.core.container.Container;

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    public MainActivityTest() {
        super(MainActivity.class);
    }

    public void testViews() {
        assertEquals("[core]", ((TextView) getActivity().findViewById(R.id.textViewComponents)).getText());
    }

    public void testDependencies() {
        //Driver.load();
        assertTrue("core not available", Container.components.contains("core"));
        assertFalse("drivers available", Container.components.contains("drivers"));
    }
}