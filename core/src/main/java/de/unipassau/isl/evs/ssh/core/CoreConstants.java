package de.unipassau.isl.evs.ssh.core;

import android.content.Intent;

import java.security.cert.X509Certificate;

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
     * API Key to Access Open Weather Map
     */
    public static final String OPENWEATHERMAP_API_KEY = "f5301a474451c6e1394268314b72a358";

    /**
     * The class contains constants for the Netty Framework
     *
     * @author Niko Fink & Phil Werli
     */
    public static class NettyConstants {
        /**
         * Default port used by netty for Connections from local network
         */
        public static final int DEFAULT_LOCAL_PORT = 13131;
        /**
         * Default port used by netty
         */
        public static final int DEFAULT_PUBLIC_PORT = 13130;

        /**
         * The time it takes in seconds after the client starts idle when reader isn't active.
         */
        public static final int READER_IDLE_TIME = 60;
        /**
         * The time it takes in seconds after the client starts idle when writer isn't active.
         */
        public static final int WRITER_IDLE_TIME = 30;
        /**
         * The time it takes in seconds after the client starts idle when reader or writer isn't active.
         * Set to infinite.
         */
        public static final int ALL_IDLE_TIME = 0;

        public static final ResourceLeakDetector.Level RESOURCE_LEAK_DETECTION = ResourceLeakDetector.Level.PARANOID;

        public static final int DISCOVERY_PROTOCOL_VERSION = 2;
        /**
         * Default discovery port used by netty
         */
        public static final int DISCOVERY_PORT = 13132;
        public static final String DISCOVERY_HOST = "255.255.255.255";
        public static final String DISCOVERY_PAYLOAD_REQUEST = "de.unipassau.isl.evs.ssh.udp_discovery.REQUEST" + DISCOVERY_PROTOCOL_VERSION;
        public static final String DISCOVERY_PAYLOAD_RESPONSE = "de.unipassau.isl.evs.ssh.udp_discovery.RESPONSE" + DISCOVERY_PROTOCOL_VERSION;

        public static final AttributeKey<X509Certificate> ATTR_PEER_CERT = AttributeKey.valueOf(X509Certificate.class.getName());
        public static final AttributeKey<DeviceID> ATTR_PEER_ID = AttributeKey.valueOf(DeviceID.class.getName());
        public static final AttributeKey<Boolean> ATTR_HANDSHAKE_FINISHED = AttributeKey.valueOf("HandshakeFinished");
        public static final AttributeKey<Boolean> ATTR_LOCAL_CONNECTION = AttributeKey.valueOf("LocalConnection");
    }

    /**
     * This class contains constants for ModuleTypes
     */
    public enum ModuleType {
        Light,
        WeatherBoard,
        DoorBuzzer,
        DoorSensor,
        WindowSensor,
        Webcam,
        Doorbell;

        @Deprecated
        public static final String LIGHT = "Light";
        @Deprecated
        public static final String WEATHER_BOARD = "WeatherBoard";
        @Deprecated
        public static final String DOOR_BUZZER = "DoorBuzzer";
        @Deprecated
        public static final String DOOR_SENSOR = "DoorSensor";
        @Deprecated
        public static final String WINDOW_SENSOR = "WindowSensor";
        @Deprecated
        public static final String WEBCAM = "Webcam";
        @Deprecated
        public static final String DOORBELL = "Doorbell";
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
    public static class QRCodeInformation {
        public static final String EXTRA_QR_DEVICE_INFORMATION = "EXTRA_QR_DEVICE_INFORMATION";
        public static final String EXTRA_QR_MESSAGE = "EXTRA_QR_MESSAGE";
        public static final int QR_CODE_IMAGE_SCALE = 35;

        public static final int REQUEST_CODE_SCAN_QR = 1;
        public static final Intent ZXING_SCAN_INTENT = new Intent("com.google.zxing.client.android.SCAN");
        public static final String ZXING_SCAN_RESULT = "SCAN_RESULT";

        static {
            ZXING_SCAN_INTENT.putExtra("SCAN_MODE", "QR_CODE_MODE");
        }
    }
}
