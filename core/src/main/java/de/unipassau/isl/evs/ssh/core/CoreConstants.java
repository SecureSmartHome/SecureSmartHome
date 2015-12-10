package de.unipassau.isl.evs.ssh.core;

import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
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

        public static final int DISCOVERY_PROTOCOL_VERSION = 2;
        /**
         * Default discovery port used by netty
         */
        public static final int DISCOVERY_PORT = 13132;
        public static final String DISCOVERY_HOST = "255.255.255.255";
        public static final String DISCOVERY_PAYLOAD_REQUEST = "de.unipassau.isl.evs.ssh.udp_discovery.REQUEST" + DISCOVERY_PROTOCOL_VERSION;
        public static final String DISCOVERY_PAYLOAD_RESPONSE = "de.unipassau.isl.evs.ssh.udp_discovery.RESPONSE" + DISCOVERY_PROTOCOL_VERSION;
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
        /**
         * @deprecated should not be written outside of NamingManager
         */
        @Deprecated
        public static final String PREF_MASTER_ID = NamingManager.PREF_MASTER_ID;
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
        public static final String MASTER_REQUEST_WEATHER_INFO = "/master/weatherinfo/request";
        public static final String MASTER_PUSH_WEATHER_INFO = "/master/weatherinfo/push";
        public static final String MASTER_NOTIFICATION_SEND = "/master/notification/send";
        public static final String MASTER_DOOR_BELL_CAMERA_GET = "/master/doorbell/camera/get";
        public static final String MASTER_DOOR_UNLATCH = "/master/door/unlatch";
        public static final String MASTER_DOOR_LOCK_SET = "/master/door/lock_set";
        public static final String MASTER_DOOR_LOCK_GET = "/master/door/lock_get";
        public static final String MASTER_DOOR_STATUS_GET = "/master/door/status_get";
        public static final String MASTER_USERINFO_GET = "/master/userinfo/get";
        public static final String MASTER_USERINFO_SET = "/master/userinfo/set";
        public static final String MASTER_HOLIDAY_SET = "/master/holiday/set";
        public static final String MASTER_HOLIDAY_GET = "/master/holiday/get";
        public static final String MASTER_MODULE_ADD = "/master/module/add";
        public static final String MASTER_MODULE_GET = "/master/module/get";
        public static final String MASTER_USER_REGISTER = "/master/user/register";
        public static final String MASTER_SLAVE_REGISTER = "/master/slave/register";
        public static final String MASTER_DEVICE_CONNECTED = "/master/device/connected";
        public static final String MASTER_MODULE_RENAME = "/master/module/modify";
        public static final String MASTER_SYSTEM_HEALTH_CHECK = "master/systemhealth/check";

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
        public static final String APP_HOLIDAY_SIMULATION = "app/holiday/get";
        public static final String APP_USERINFO_GET = "/app/userdevice/get";
        public static final String APP_MODULE_ADD = "/app/module/add";
        public static final String APP_USER_REGISTER = "/app/user/register";
        public static final String APP_SLAVE_REGISTER = "/app/slave/register";

        // Slave/App (used for broadcast messages)
        public static final String MODULES_UPDATE = "/*/modules/update";
        public static final String APP_REQUEST_WEATHER_INFO = "/app/weatherinfo/request";
    }

    /**
     * @author leon
     */
    public class LogActions {
        public static final String LIGHT_ON_ACTION = "LightOn";
        public static final String LIGHT_OFF_ACTION = "LightOff";
    }

    /**
     * This class contains constants for the information sent to create and display a QR-Code.
     *
     * @author Phil Werli
     */
    public class QRCodeInformation {
        public static final String EXTRA_QR_DEVICE_INFORMATION = "EXTRA_QR_DEVICE_INFORMATION";
        public static final String EXTRA_QR_MESSAGE = "EXTRA_QR_MESSAGE";
        public static final int QR_CODE_IMAGE_SCALE = 35;
    }

    /**
     * @author Wolfgang Popp
     * @author leon
     */
    public static class Permission {

        public static class TernaryPermission {
            public static final String SWITCH_LIGHT = "SwitchLight";

            public static String[] getPermissions(String moduleType) {
                switch (moduleType) {
                    case ModuleType.LIGHT:
                        return new String[]{SWITCH_LIGHT};
                    default:
                        return null;
                }
            }

        }

        public enum BinaryPermission {

            //Odroid
            ADD_ODROID("AddOdroid"),
            RENAME_ODROID("RenameOdroid"),
            DELETE_ODROID("DeleteOdroid"),

            //Sensor
            ADD_SENSOR("AddSensor"),
            RENAME_MODULE("RenameSensor"),
            DELETE_SENSOR("DeleteSensor"),

            //Light
            REQUEST_LIGHT_STATUS("RequestLightStatus"),

            // Window
            REQUEST_WINDOW_STATUS("RequestWindowStatus"),

            //Door
            REQUEST_DOOR_STATUS("RequestDoorStatus"),
            LOCK_DOOR("LockDoor"),
            UNLATCH_DOOR("UnlatchDoor"),

            //Camera
            REQUEST_CAMERA_STATUS("RequestCameraStatus"),
            TAKE_CAMERA_PICTURE("TakeCameraPicture"),

            //WeaterStation
            REQUEST_WEATHER_STATUS("RequestWeatherStatus"),

            //HolidaySimulation
            START_HOLIDAY_SIMULATION("StartHolidaySimulation"),
            STOP_HOLIDAY_SIMULATION("StopHolidaySimulation"),

            //User
            ADD_USER("AddUser"),
            DELETE_USER("DeleteUser"),
            CHANGE_USER_NAME("ChangeUserName"),
            CHANGE_USER_GROUP("ChangeUserGroup"),
            GRANT_USER_PERMISSION("GrantUserPermission"),
            WITHDRAW_USER_PERMISSION("WithdrawUserPermission"),

            //Groups
            ADD_GROUP("AddGroup"),
            DELETE_GROUP("DeleteGroup"),
            CHANGE_GROUP_NAME("ChangeGroupName"),
            SHOW_GROUP_MEMBER("ShowGroupMembers"),
            CHANGE_GROUP_TEMPLATE("ChangeGroupTemplate"),

            //Templates
            CREATE_TEMPLATE("CreateTemplate"),
            DELETE_TEMPLATE("DeleteTemplate"),
            EDIT_TEMPLATE("EditTemplate"),
            SHOW_TEMPLATE_PERMISSION("ShowTemplatePermission"),

            //Notification Types
            ODROID_ADDED("OdroidAdded"),
            HUMIDITY_WARNING("HumidityWarning"),
            BRIGHTNESS_WARNING("BrightnessWarning"),
            HOLIDAY_MODE_SWITCHED_ON("HolidayModeSwitchedOn"),
            HOLIDAY_MODE_SWITCHED_OFF("HolidayModeSwitchedOff"),
            SYSTEM_HEALTH_WARNING("SystemHealthWarning"),
            BELL_RANG("BellRang"),
            WEATHER_WARNING("WeatherWarning"),
            DOOR_UNLATCHED("DoorOpened"),
            DOOR_LOCKED("DoorLocked"),
            DOOR_UNLOCKED("DoorUnlocked");

            private String name;

            BinaryPermission(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                return name;
            }
        }
    }
}
