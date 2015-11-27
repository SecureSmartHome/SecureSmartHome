package de.unipassau.isl.evs.ssh.app;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.TextView;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.drivers.Driver;
import de.unipassau.isl.evs.ssh.slave.MainActivity;
import de.unipassau.isl.evs.ssh.slave.R;

/**
 * ActivityInstrumentationTestCase2 for the main activity.
 *
 * @author Niko
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    public MainActivityTest() {
        super(MainActivity.class);
    }

    public void testViews() {
        assertEquals("[core, drivers]", ((TextView) getActivity().findViewById(R.id.textViewComponents)).getText());
    }

    public void testDependencies() {
        Driver.load();
        assertTrue("core not available", Container.components.contains("core"));
        assertTrue("drivers not available", Container.components.contains("drivers"));
    }
}