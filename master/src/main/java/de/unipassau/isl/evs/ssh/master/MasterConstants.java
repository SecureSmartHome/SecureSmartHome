package de.unipassau.isl.evs.ssh.master;

/**
 * This Constants class provides constants needed by the master module.
 *
 * @author Team
 */
public enum MasterConstants {
    ;

    /**
     * Containing thresholds for climate data. If thresholds are bypassed a warning should be sent.
     *
     * @author Christoph Fraedrich
     */
    public enum ClimateThreshold {
        ;

        public static final int ALTITUDE = 100;
        //Threshold Humidity 80%. Mold will start to grow at this value.
        public static final double HUMIDITY = 80;
        //Normal air pressure at sea level.
        public static final double PRESSURE = 101325;
        public static final double TEMP1 = 21;
        public static final double TEMP2 = 25;
        //Bright Day 100 Lux. Cloudy Day 20 Lux. -> Threshold 60 Lux
        public static final int VISIBLE_LIGHT = 60;
    }
}
