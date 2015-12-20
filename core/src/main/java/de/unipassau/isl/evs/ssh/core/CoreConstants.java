package de.unipassau.isl.evs.ssh.core;

import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.AddNewModulePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorLockPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorStatusPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorUnlatchPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.GenerateNewRegisterTokenPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.HolidaySimulationPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.RegisterSlavePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.RenameModulePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SystemHealthPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.UserDeviceInformationPayload;
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
    public static class RoutingKeys {
        private RoutingKeys() {}

        private static final String PREFIX_MASTER = "/master";
        private static final String PREFIX_SLAVE = "/slave";
        private static final String PREFIX_APP = "/app";
        private static final String PREFIX_GLOBAL = "/global";

        // Master
        public static final RoutingKey<LightPayload> MASTER_LIGHT_GET = new RoutingKey<>(PREFIX_MASTER + "/light/get", LightPayload.class);
        public static final RoutingKey<LightPayload> MASTER_LIGHT_SET = new RoutingKey<>(PREFIX_MASTER + "/light/set", LightPayload.class);
        public static final RoutingKey<DoorBellPayload> MASTER_DOOR_BELL_RING = new RoutingKey<>(PREFIX_MASTER + "/doorbell/ring", DoorBellPayload.class);
        public static final RoutingKey<CameraPayload> MASTER_CAMERA_GET = new RoutingKey<>(PREFIX_MASTER + "/camera/get", CameraPayload.class);
        public static final RoutingKey<MessagePayload> MASTER_REQUEST_WEATHER_INFO = new RoutingKey<>(PREFIX_MASTER + "/weatherinfo/request", MessagePayload.class);
        public static final RoutingKey<ClimatePayload> MASTER_PUSH_WEATHER_INFO = new RoutingKey<>(PREFIX_MASTER + "/weatherinfo/push", ClimatePayload.class);
        public static final RoutingKey<NotificationPayload> MASTER_NOTIFICATION_SEND = new RoutingKey<>(PREFIX_MASTER + "/notification/send", NotificationPayload.class);
        public static final RoutingKey<CameraPayload> MASTER_DOOR_BELL_CAMERA_GET = new RoutingKey<>(PREFIX_MASTER + "/doorbell/camera/get", CameraPayload.class);
        public static final RoutingKey<DoorUnlatchPayload> MASTER_DOOR_UNLATCH = new RoutingKey<>(PREFIX_MASTER + "/door/unlatch", DoorUnlatchPayload.class);
        public static final RoutingKey<DoorLockPayload> MASTER_DOOR_LOCK_SET = new RoutingKey<>(PREFIX_MASTER + "/door/lock_set", DoorLockPayload.class);
        public static final RoutingKey<DoorLockPayload> MASTER_DOOR_LOCK_GET = new RoutingKey<>(PREFIX_MASTER + "/door/lock_get", DoorLockPayload.class);
        public static final RoutingKey<DoorStatusPayload> MASTER_DOOR_STATUS_GET = new RoutingKey<>(PREFIX_MASTER + "/door/status_get", DoorStatusPayload.class);
        public static final RoutingKey<MessagePayload> MASTER_USERINFO_GET = new RoutingKey<>(PREFIX_MASTER + "/userinfo/get", MessagePayload.class);
        public static final RoutingKey<MessagePayload> MASTER_USERINFO_SET = new RoutingKey<>(PREFIX_MASTER + "/userinfo/set", MessagePayload.class);
        public static final RoutingKey<HolidaySimulationPayload> MASTER_HOLIDAY_SET = new RoutingKey<>(PREFIX_MASTER + "/holiday/set", HolidaySimulationPayload.class);
        public static final RoutingKey<HolidaySimulationPayload> MASTER_HOLIDAY_GET = new RoutingKey<>(PREFIX_MASTER + "/holiday/get", HolidaySimulationPayload.class);
        public static final RoutingKey<AddNewModulePayload> MASTER_MODULE_ADD = new RoutingKey<>(PREFIX_MASTER + "/module/add", AddNewModulePayload.class);
        public static final RoutingKey<MessagePayload> MASTER_MODULE_GET = new RoutingKey<>(PREFIX_MASTER + "/module/get", MessagePayload.class);
        public static final RoutingKey<GenerateNewRegisterTokenPayload> MASTER_USER_REGISTER = new RoutingKey<>(PREFIX_MASTER + "/user/register", GenerateNewRegisterTokenPayload.class);
        public static final RoutingKey<RegisterSlavePayload> MASTER_SLAVE_REGISTER = new RoutingKey<>(PREFIX_MASTER + "/slave/register", RegisterSlavePayload.class);
        public static final RoutingKey<MessagePayload> MASTER_DEVICE_CONNECTED = new RoutingKey<>(PREFIX_MASTER + "/device/connected", MessagePayload.class);
        public static final RoutingKey<RenameModulePayload> MASTER_MODULE_RENAME = new RoutingKey<>(PREFIX_MASTER + "/module/modify", RenameModulePayload.class);
        public static final RoutingKey<SystemHealthPayload> MASTER_SYSTEM_HEALTH_CHECK = new RoutingKey<>(PREFIX_MASTER + "/systemhealth/check", SystemHealthPayload.class);

        // Slave
        public static final RoutingKey<MessagePayload> SLAVE_LIGHT_GET = new RoutingKey<>(PREFIX_SLAVE + "/light/get", MessagePayload.class);
        public static final RoutingKey<LightPayload> SLAVE_LIGHT_SET = new RoutingKey<>(PREFIX_SLAVE + "/light/set", LightPayload.class);
        public static final RoutingKey<CameraPayload> SLAVE_CAMERA_GET = new RoutingKey<>(PREFIX_SLAVE + "/camera/get", CameraPayload.class);
        public static final RoutingKey<MessagePayload> SLAVE_DOOR_STATUS_GET = new RoutingKey<>(PREFIX_SLAVE + "/door/status_get", MessagePayload.class);
        public static final RoutingKey<DoorUnlatchPayload> SLAVE_DOOR_UNLATCH = new RoutingKey<>(PREFIX_SLAVE + "/door/unlatch", DoorUnlatchPayload.class);
        public static final RoutingKey<MessagePayload> SLAVE_MODULES_UPDATE = new RoutingKey<>(PREFIX_SLAVE + "/modules/update", MessagePayload.class);

        // App
        public static final RoutingKey<MessagePayload> APP_MODULES_GET = new RoutingKey<>(PREFIX_APP + "/module/get", MessagePayload.class);
        public static final RoutingKey<LightPayload> APP_LIGHT_UPDATE = new RoutingKey<>(PREFIX_APP + "/light/update", LightPayload.class);
        public static final RoutingKey<ClimatePayload> APP_CLIMATE_UPDATE = new RoutingKey<>(PREFIX_APP + "/climate/update", ClimatePayload.class);
        public static final RoutingKey<MessagePayload> APP_NOTIFICATION_RECEIVE = new RoutingKey<>(PREFIX_APP + "/notification/receive", MessagePayload.class);
        public static final RoutingKey<MessagePayload> APP_NOTIFICATION_PICTURE_RECEIVE = new RoutingKey<>(PREFIX_APP + "/notification/picture_receive", MessagePayload.class);
        public static final RoutingKey<CameraPayload> APP_CAMERA_GET = new RoutingKey<>(PREFIX_APP + "/camera/get", CameraPayload.class);
        public static final RoutingKey<DoorLockPayload> APP_DOOR_BLOCK = new RoutingKey<>(PREFIX_APP + "/door/block", DoorLockPayload.class);
        public static final RoutingKey<DoorStatusPayload> APP_DOOR_GET = new RoutingKey<>(PREFIX_APP + "/door/get", DoorStatusPayload.class);
        public static final RoutingKey<DoorBellPayload> APP_DOOR_RING = new RoutingKey<>(PREFIX_APP + "/door/ring", DoorBellPayload.class);
        public static final RoutingKey<MessagePayload> APP_HOLIDAY_SIMULATION = new RoutingKey<>(PREFIX_APP + "/holiday/get", MessagePayload.class);
        public static final RoutingKey<UserDeviceInformationPayload> APP_USERINFO_GET = new RoutingKey<>(PREFIX_APP + "/userdevice/get", UserDeviceInformationPayload.class);
        public static final RoutingKey<Void> APP_MODULE_ADD = new RoutingKey<>(PREFIX_APP + "/module/add", Void.class);
        public static final RoutingKey<GenerateNewRegisterTokenPayload> APP_USER_REGISTER = new RoutingKey<>(PREFIX_APP + "/user/register", GenerateNewRegisterTokenPayload.class);
        public static final RoutingKey<MessagePayload> APP_SLAVE_REGISTER = new RoutingKey<>(PREFIX_APP + "/slave/register", MessagePayload.class);

        // Global
        public static final RoutingKey<ModulesPayload> GLOBAL_MODULES_UPDATE = new RoutingKey<>(PREFIX_GLOBAL + "/modules/update", ModulesPayload.class);
        public static final RoutingKey<MessagePayload> GLOBAL_REQUEST_WEATHER_INFO = new RoutingKey<>(PREFIX_GLOBAL + "/weatherinfo/request", MessagePayload.class);
        public static final RoutingKey<MessagePayload> GLOBAL_DEMO = new RoutingKey<>(PREFIX_GLOBAL + "/demo", MessagePayload.class);
    }

    /**
     * @author Leon Sell
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
}
