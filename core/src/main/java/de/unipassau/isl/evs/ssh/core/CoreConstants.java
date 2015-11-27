package de.unipassau.isl.evs.ssh.core;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import io.netty.util.AttributeKey;
import io.netty.util.ResourceLeakDetector;

import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * This Constants class provides constants needed by all modules.
 *
 * @author Team
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

    public static final AttributeKey<X509Certificate> ATTR_CLIENT_CERT = AttributeKey.valueOf(X509Certificate.class.getName());
    public static final AttributeKey<DeviceID> ATTR_CLIENT_ID = AttributeKey.valueOf(DeviceID.class.getName());

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

    /**
     * @author leon
     */
    public class LogActions {
        public static final String LIGHT_ON_ACTION = "LightOn";
        public static final String LIGHT_OFF_ACTION = "LightOff";
    }

    /**
     * @author leon
     */
    public class NotificationTypes {
        public static final String ODROID_ADDED = "OdroidAdded";
        public static final String HUMIDITY_WARNING = "HumidityWarning";
        public static final String BRIGHTNESS_WARNING = "BrightnessWarning";
        public static final String HOLIDAY_MODE_SWITCHED_ON = "HolidayModeSwitchedOn";
        public static final String HOLIDAY_MODE_SWITCHED_OFF = "HolidayModeSwitchedOff";
        public static final String SYSTEM_HEALT_WARNING = "SystemHealthWarning";
        public static final String BELL_RANG = "BellRang";
        public static final String WEATHER_WARNING = "WeatherWarning";
        public static final String DOOR_UNLATCHED = "DoorOpened";
        public static final String DOOR_LOCKED = "DoorLocked";
        public static final String DOOR_UNLOCKED = "DoorUnlocked";
    }
}
