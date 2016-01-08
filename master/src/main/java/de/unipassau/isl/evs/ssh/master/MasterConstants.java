package de.unipassau.isl.evs.ssh.master;

import de.unipassau.isl.evs.ssh.core.CoreConstants;

/**
 * This Constants class provides constants needed by the master module.
 *
 * @author Team
 */
public class MasterConstants extends CoreConstants {
    /**
     * @author Christoph Fraedrich
     */
    public class ClimateThreshold {
        public static final int ALTITUDE = 100;
        //Threshold Humidity 80%. Mold will start to grow at this value.
        public static final double HUMIDITY = 80;
        //Normal air pressure at sea level.
        public static final double PRESSURE = 101325;
        public static final double TEMP1 = 21;
        public static final double TEMP2 = 25;
        //Bright Day 100.000 Lux. Cloudy Day 20.000 Lux. -> Threshold 60.000 Lux
        public static final int VISIBLE_LIGHT = 60000;
    }
}
