package de.unipassau.isl.evs.ssh.master;

import de.unipassau.isl.evs.ssh.core.CoreConstants;

/**
 * This Constants class provides constants needed by the master module.
 *
 * @author Team
 */
public class MasterConstants extends CoreConstants {
    public static final String PREF_SERVER_PORT = "PREF_SERVER_PORT";

    public class ClimateThreshold {
        public static final int ALTITUDE = 100;
        public static final double HUMIDITY = 100;
        public static final double PRESSURE = 100;
        public static final double TEMP1 = 21;
        public static final double TEMP2 = 25;
        public static final int VISIBLE_LIGHT = 500;
    }
}
