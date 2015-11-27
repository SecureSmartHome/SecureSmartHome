package de.unipassau.isl.evs.ssh.drivers;

import android.util.Log;
import de.unipassau.isl.evs.ssh.core.container.Container;

/**
 * Driver class used to trigger loading of drivers
 *
 * @author Niko
 */
public class Driver {
    static {
        Log.i("Driver", "class load");
        load();
    }

    public static void load() {
        Log.i("Driver", "manual load");
        Container.components.add("drivers");
    }
}
