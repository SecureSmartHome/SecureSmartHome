package de.unipassau.isl.evs.ssh.core;

import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import io.netty.util.AttributeKey;
import io.netty.util.ResourceLeakDetector;

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
     * The class contains constants for the Netty Framework
     *
     * @author Niko Fink & Phil Werli
     */
    public static class NettyConstants {

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

        public static final ResourceLeakDetector.Level RESOURCE_LEAK_DETECTION = ResourceLeakDetector.Level.PARANOID;

        /**
         * Default discovery port used by netty
         */
        public static final int DISCOVERY_PORT = 13132;
        public static final String DISCOVERY_HOST = "255.255.255.255";
        public static final String DISCOVERY_PAYLOAD_REQUEST = "de.unipassau.isl.evs.ssh.udp_discovery.REQUEST";
        public static final String DISCOVERY_PAYLOAD_RESPONSE = "de.unipassau.isl.evs.ssh.udp_discovery.RESPONSE";
        public static final String[] DISCOVERY_PAYLOADS = {DISCOVERY_PAYLOAD_REQUEST, DISCOVERY_PAYLOAD_RESPONSE};

        public static final AttributeKey<X509Certificate> ATTR_PEER_CERT = AttributeKey.valueOf(X509Certificate.class.getName());
        public static final AttributeKey<DeviceID> ATTR_PEER_ID = AttributeKey.valueOf(DeviceID.class.getName());
    }

    public static class Security {
        public static final String MESSAGE_CRYPT_ALG = "ECIES";
        public static final String MESSAGE_SIGN_ALG = "SHA224withECDSA";
    }

    /**
     * This class contains the key constants of SharedPreferences
     */
    public class SharedPrefs {
        public static final String PREF_MASTER_ID = "ssh.core.MASTER_ID";
        public static final String PREF_TOKEN = "ssh.core.TOKEN"; //TODO check if this is the correct place
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
        public static final String DOORBELL = "Doorbell";
    }

    /**
     * This class contains constants for RoutingKeys
     */
    public class RoutingKeys {
        //Master
        public static final String MASTER_LIGHT_GET = "/master/light/get";
        public static final String MASTER_LIGHT_SET = "/master/light/set";
        public static final String MASTER_DOOR_BELL_RING = "/master/doorbell/ring";
        public static final String MASTER_CAMERA_GET = "/master/camera/get";
        public static final String MASTER_WEATHER_INFO = "/master/weather/info";
        public static final String MASTER_NOTIFICATION_SEND = "/master/notification/send";
        public static final String MASTER_DOOR_BELL_CAMERA_GET = "/master/doorbell/camera/get";
        public static final String MASTER_DOOR_UNLATCH = "/master/door/unlatch";
        public static final String MASTER_DOOR_LOCK_SET = "/master/door/lock_set";
        public static final String MASTER_DOOR_LOCK_GET = "/master/door/lock_get";
        public static final String MASTER_DOOR_STATUS_GET = "/master/door/status_get";
        public static final String MASTER_USERINFO_GET = "/master/userinfo/get";
        public static final String MASTER_USERINFO_SET = "/master/userinfo/set";
        public static final String MASTER_MODULE_ADD = "/master/module/add";
        public static final String MASTER_MODULE_GET = "/master/module/get";
        public static final String MASTER_REGISTER_INIT = "/master/register/init";
        public static final String MASTER_REGISTER_FINALIZE = "/master/register/finalize";
        public static final String MASTER_DEVICE_CONNECTED = "/master/device/connected";

        //Slave
        public static final String SLAVE_LIGHT_GET = "/slave/light/get";
        public static final String SLAVE_LIGHT_SET = "/slave/light/set";
        public static final String SLAVE_CAMERA_GET = "/slave/camera/get";
        public static final String SLAVE_DOOR_STATUS_GET = "/slave/door/status_get";
        public static final String SLAVE_DOOR_UNLATCH = "/slave/door/unlatch";
        public static final String SLAVE_MODULES_UPDATE = "/slave/modules/update";

        //App
        public static final String APP_MODULES_GET = "/app/module/get";
        public static final String APP_LIGHT_UPDATE = "/app/light/update";
        public static final String APP_CLIMATE_UPDATE = "/app/climate/update";
        public static final String APP_NOTIFICATION_RECEIVE = "/app/notification/receive";
        public static final String APP_NOTIFICATION_PICTURE_RECEIVE = "/app/notification/picture_receive";
        public static final String APP_CAMERA_GET = "/app/camera/get";
        public static final String APP_DOOR_BLOCK = "/app/door/block";
        public static final String APP_DOOR_GET = "/app/door/get";
        public static final String APP_DOOR_RING = "/app/door/ring";
        public static final String APP_USERINFO_GET = "/app/userdevice/get";
        public static final String APP_MODULE_ADD = "/app/module/add";
        public static final String APP_USER_REGISTER = "/app/user/register";

        // Slave/App (used for broadcast messages)
        public static final String MODULES_UPDATE = "/*/modules/update";
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
        //Each type also acts as a permission in the database
        public static final String ODROID_ADDED = "OdroidAdded";
        public static final String HUMIDITY_WARNING = "HumidityWarning";
        public static final String BRIGHTNESS_WARNING = "BrightnessWarning";
        public static final String HOLIDAY_MODE_SWITCHED_ON = "HolidayModeSwitchedOn";
        public static final String HOLIDAY_MODE_SWITCHED_OFF = "HolidayModeSwitchedOff";
        public static final String SYSTEM_HEALTH_WARNING = "SystemHealthWarning";
        public static final String BELL_RANG = "BellRang";
        public static final String WEATHER_WARNING = "WeatherWarning";
        public static final String DOOR_UNLATCHED = "DoorOpened";
        public static final String DOOR_LOCKED = "DoorLocked";
        public static final String DOOR_UNLOCKED = "DoorUnlocked";
    }

    /**
     * This class contains constants for the information sent to create and display a QR-Code.
     *
     * @author Phil Werli
     */
    public class QRCodeInformation {
        public static final String EXTRA_QR_DEVICE_INFORMATION = "EXTRA_QR_DEVICE_INFORMATION";
        public static final String EXTRA_QR_MESSAGE = "EXTRA_QR_MESSAGE";
    }
}
