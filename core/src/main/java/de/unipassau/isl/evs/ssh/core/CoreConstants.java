/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unipassau.isl.evs.ssh.core;

import android.content.Context;
import android.content.Intent;

import java.security.cert.X509Certificate;

import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.GPIOAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.MockAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.ModuleAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.USBAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.WLANAccessPoint;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import io.netty.util.AttributeKey;
import io.netty.util.ResourceLeakDetector;

/**
 * This Constants class provides constants needed by all modules.
 *
 * @author Team
 */
public enum CoreConstants {
    ;
    /**
     * Name used for SharedPreferences file
     */
    public static final String FILE_SHARED_PREFS = "shared-preferences";

    /**
     * API Key to Access Open Weather Map
     */
    public static final String OPENWEATHERMAP_API_KEY = "f5301a474451c6e1394268314b72a358";

    /**
     * {@code true}, if statistics about the delay between receiving requests and sending the corresponding response
     * should be logged. Parsed from String so that IDEs don't nag about constant expressions.
     */
    public static final boolean TRACK_STATISTICS = Boolean.parseBoolean("true");

    /**
     * The class contains constants for the Netty Framework
     *
     * @author Niko Fink & Phil Werli
     */
    public enum NettyConstants {
        ;
        /**
         * Default port used by netty for connections from local network
         */
        public static final int DEFAULT_LOCAL_PORT = 13131;
        /**
         * Default port used by netty for connections from the internet
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

        /**
         * ResourceLeakDetector used for unreleased Netty Buffers
         */
        public static final ResourceLeakDetector.Level RESOURCE_LEAK_DETECTION =
                BuildConfig.DEBUG ? ResourceLeakDetector.Level.PARANOID : ResourceLeakDetector.Level.SIMPLE;

        /**
         * Protocol identifier for the discovery protocol so that only devices which also understand each other can find
         * each other via UDP discovery.
         */
        public static final int DISCOVERY_PROTOCOL_VERSION = 2;
        /**
         * Default discovery port used by netty
         */
        public static final int DISCOVERY_SERVER_PORT = 13132;
        /**
         * UDP broadcast address
         */
        public static final String DISCOVERY_HOST = "255.255.255.255";
        /**
         * Identifier for an UDP discovery request
         */
        public static final String DISCOVERY_PAYLOAD_REQUEST = "de.unipassau.isl.evs.ssh.udp_discovery.REQUEST" + DISCOVERY_PROTOCOL_VERSION;
        /**
         * Identifier for an UDP discovery response
         */
        public static final String DISCOVERY_PAYLOAD_RESPONSE = "de.unipassau.isl.evs.ssh.udp_discovery.RESPONSE" + DISCOVERY_PROTOCOL_VERSION;

        /**
         * The certificate of the peer device, set after it is verified by the handshake
         */
        public static final AttributeKey<X509Certificate> ATTR_PEER_CERT = AttributeKey.valueOf(X509Certificate.class.getName());
        /**
         * The DeviceID of the peer device, set after it is verified by the handshake
         */
        public static final AttributeKey<DeviceID> ATTR_PEER_ID = AttributeKey.valueOf(DeviceID.class.getName());
        /**
         * Whether the handshake is finished, the peer device is authenticated and ATTR_PEER_CERT and ATTR_PEER_ID are set.
         */
        public static final AttributeKey<Boolean> ATTR_HANDSHAKE_FINISHED = AttributeKey.valueOf("HandshakeFinished");
        /**
         * Whether the Client is connected to the local port or via internet.
         */
        public static final AttributeKey<Boolean> ATTR_LOCAL_CONNECTION = AttributeKey.valueOf("LocalConnection");
    }

    /**
     * This class contains constants for ModuleTypes
     */
    public enum ModuleType {
        Light(R.string.module_type_light),
        WeatherBoard(R.string.module_type_weather_board),
        DoorBuzzer(R.string.module_type_door_buzzer),
        DoorSensor(R.string.module_type_door_sensor),
        WindowSensor(R.string.module_type_window_sensor),
        Webcam(R.string.module_type_webcam),
        Doorbell(R.string.module_type_doorbell);

        private final int resID;

        ModuleType(int resID) {
            this.resID = resID;
        }

        public String toLocalizedString(Context ctx) {
            return ctx.getResources().getString(this.resID);
        }

        public boolean isValidAccessPoint(ModuleAccessPoint accessPoint) {
            switch (this) {
                case Light:
                    return accessPoint.getType().equals(WLANAccessPoint.TYPE);
                case WeatherBoard:
                    return accessPoint.getType().equals(USBAccessPoint.TYPE);
                case DoorBuzzer:
                    return accessPoint.getType().equals(GPIOAccessPoint.TYPE);
                case DoorSensor:
                    return accessPoint.getType().equals(GPIOAccessPoint.TYPE);
                case WindowSensor:
                    return accessPoint.getType().equals(GPIOAccessPoint.TYPE);
                case Webcam:
                    return accessPoint.getType().equals(USBAccessPoint.TYPE)
                            || accessPoint.getType().equals(MockAccessPoint.TYPE);
                case Doorbell:
                    return accessPoint.getType().equals(GPIOAccessPoint.TYPE);
                default:
                    return false;
            }
        }
    }

    /**
     * Actions logged for Holiday simulation
     *
     * @author Leon Sell
     */
    public enum LogActions {
        ;
        public static final String LIGHT_ON_ACTION = "LightOn";
        public static final String LIGHT_OFF_ACTION = "LightOff";
    }

    /**
     * This class contains constants for the information sent to create and display a QR-Code.
     *
     * @author Phil Werli
     */
    public enum QRCodeInformation {
        ;
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
