package de.unipassau.isl.evs.ssh.core;

import java.util.concurrent.TimeUnit;

import io.netty.util.ResourceLeakDetector;

/**
 * This Constants class provides constants needed by all modules.
 */
public class CoreConstants {
    /**
     * Name used for SharedPreferences file
     */
    public static final String FILE_SHARED_PREFS = "shared-preferences";
    /**
     * Default port used by netty
     */
    public static final int DEFAULT_PORT = 13131;
    /**
     * Preferred port used by netty
     */
    public static final String PREF_PORT = "PREF_PORT";
    /**
     * Preferred host used by netty, default is {@code null}
     */
    public static final String PREF_HOST = "PREF_HOST";
    /**
     * The time it takes in seconds after the client starts idle when reader isn't active.
     */
    public static final int CLIENT_READER_IDLE_TIME = 60;
    /**
     * The time it takes in seconds after the client starts idle when writer isn't active.
     */
    public static final int CLIENT_WRITER_IDLE_TIME = 30;
    /**
     * The time it takes in seconds after the client starts idle when reader or writer isn't active.
     * Set to infinite.
     */
    public static final int CLIENT_ALL_IDLE_TIME = 0;
    /**
     * Default value for maximum timeouts.
     */
    public static final int CLIENT_MAX_DISCONNECTS = 3;
    /**
     * The minimum number of seconds between
     */
    public static final long CLIENT_MILLIS_BETWEEN_DISCONNECTS = TimeUnit.SECONDS.toMillis(10);
    /**
     * The maximum number of seconds the broadcast waits to be sent again.
     */
    public static final long CLIENT_MILLIS_BETWEEN_BROADCASTS = TimeUnit.SECONDS.toMillis(2);
    /**
     * Default port used by netty
     */
    public static final int DISCOVERY_PORT = 13132;

    public static final String DISCOVERY_PAYLOAD_REQUEST = "de.unipassau.isl.evs.ssh.udp_discovery.REQUEST";
    public static final String DISCOVERY_PAYLOAD_RESPONSE = "de.unipassau.isl.evs.ssh.udp_discovery.RESPONSE";
    public static final ResourceLeakDetector.Level RESOURCE_LEAK_DETECTION = ResourceLeakDetector.Level.PARANOID;
    public static final String DISCOVERY_HOST = "255.255.255.255";

    /**
     * This class contains the key constants of SharedPreferences
     */
    public class SharedPrefs {
        public static final String PREF_MASTER_ID = "ssh.core.MASTER_ID";
    }

    /**
     * This class contains constants for ModuleTypes
     */
    public class ModuleType {
        public static final String LIGHT = "Light";
        public static final String WEATHER_BOARD = "WeatherBoard";
        public static final String DOOR_BUZZER = "DoorBuzzer";
        public static final String DOOR_SENSOR = "DoorSensor";
        public static final String WINDOW_SENSOR = "WindowSensor";
        public static final String WEBCAM = "Webcam";
    }

    /**
     * This class contains constants for RoutingKeys
     */
    public class RoutingKeys {
        //Master
        public static final String MASTER_LIGHT_GET = "/master/light/get";
        public static final String MASTER_LIGHT_SET = "/master/light/set";

        //Slave
        public static final String SLAVE_LIGHT_GET = "/slave/light/get";
        public static final String SLAVE_LIGHT_SET = "/slave/light/set";

        //App
        public static final String APP_MODULES_GET = "/app/module/get";
    }
}
