package de.unipassau.isl.evs.ssh.app.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.APP_CLIMATE_UPDATE;

/**
 * AppClimateHandler class handles message from and to the
 * {@link de.unipassau.isl.evs.ssh.app.activity.ClimateFragment ClimateFragment}
 *
 * @author Andreas Bucher
 */
public class AppClimateHandler extends AbstractMessageHandler implements Component {
    public static final Key<AppClimateHandler> KEY = new Key<>(AppClimateHandler.class);

    private static final long REFRESH_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(1);
    private final List<ClimateHandlerListener> listeners = new ArrayList<>();
    private final Map<Module, ClimateStatus> climateStatusMapping = new HashMap<>();

    /**
     * Request current weatherSensor data.
     *
     * @param payload MessagePayload
     * @param message MessageContent
     */
    public void toggleClimate(ClimatePayload payload, String message) {
        setClimate(payload, message);
    }

    /**
     * Links the sensorData to a Module. If the data is already linked to the Module, it refreshes the Data.
     *
     * @param module   to which the data is linked to
     * @param temp1    Temperature1
     * @param temp2    Temperature2
     * @param pressure AirPressure
     * @param altitude Altitude
     * @param humidity Humidity
     * @param uv       UV radiation
     * @param ir       IR radiation
     * @param visible  light intensity
     */
    private void setCachedStatus(Module module, double temp1, double temp2, double pressure, double altitude,
                                 double humidity, double uv, int ir, int visible) {
        ClimateStatus status = climateStatusMapping.get(module);
        if (status == null) {
            status = new ClimateStatus(temp1, temp2, pressure, altitude, humidity, uv, ir, visible);
            climateStatusMapping.put(module, status);
        } else {
            status.setStatus(temp1, temp2, pressure, altitude, humidity, uv, ir, visible);
        }
        for (ClimateHandlerListener listener : listeners) {
            listener.statusChanged(module);
        }
    }

    /**
     * Return temperature that is measured in Temperature1 sensor.
     *
     * @param module The weatherboard which sensors are measured.
     * @return The temperature measured in Temperature1 sensor.
     */
    public double getTemp1(Module module) {
        final ClimateStatus status = climateStatusMapping.get(module);
        if (System.currentTimeMillis() - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            requestClimateStatus(module);
        }
        return status.getTemp1();
    }

    /**
     * Return temperature that is measured in Temperature2 sensor.
     *
     * @param module The weatherboard which sensors is measured.
     * @return The temperature measured in Temperature2 sensor.
     */
    public double getTemp2(Module module) {
        final ClimateStatus status = climateStatusMapping.get(module);
        if (System.currentTimeMillis() - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            requestClimateStatus(module);
        }
        return status.getTemp2();
    }

    /**
     * Return pressure that is measured in AirPressure sensor.
     *
     * @param module The weatherboard which sensor is measured.
     * @return The temperature measured in AirPressure sensor.
     */
    public double getPressure(Module module) {
        final ClimateStatus status = climateStatusMapping.get(module);
        if (System.currentTimeMillis() - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            requestClimateStatus(module);
        }
        return status.getPressure();
    }

    /**
     * Return altitude that is measured in AirPressure sensor.
     *
     * @param module The weatherboard which sensor is measured.
     * @return The temperature measured in Altitude sensor.
     */
    public double getAltitude(Module module) {
        final ClimateStatus status = climateStatusMapping.get(module);
        if (System.currentTimeMillis() - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            requestClimateStatus(module);
        }
        return status.getAltitude();
    }

    /**
     * Return humidity that is measured in Humidity sensor.
     *
     * @param module The weatherboard which sensor is measured.
     * @return The temperature measured in Humidity sensor.
     */
    public double getHumidity(Module module) {
        final ClimateStatus status = climateStatusMapping.get(module);
        if (System.currentTimeMillis() - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            requestClimateStatus(module);
        }
        return status.getHumidity();
    }

    /**
     * Return uv radiation that is measured in UV Radiation sensor.
     *
     * @param module The weatherboard which sensor is measured.
     * @return The temperature measured in UV Radiation sensor.
     */
    public double getUv(Module module) {
        final ClimateStatus status = climateStatusMapping.get(module);
        if (System.currentTimeMillis() - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            requestClimateStatus(module);
        }
        return status.getUv();
    }

    /**
     * Return ir radiation that is measured in IR Radiation sensor.
     *
     * @param module The weatherboard which sensor is measured.
     * @return The temperature measured in IR Radiation sensor.
     */
    public int getVisible(Module module) {
        final ClimateStatus status = climateStatusMapping.get(module);
        if (System.currentTimeMillis() - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            requestClimateStatus(module);
        }
        return status.getVisible();
    }

    /**
     * Return light intensity that is measured in Light Intensity sensor.
     *
     * @param module The weatherboard which sensor is measured.
     * @return The temperature measured in Light Intensity sensor.
     */
    public int getIr(Module module) {
        final ClimateStatus status = climateStatusMapping.get(module);
        if (System.currentTimeMillis() - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            requestClimateStatus(module);
        }
        return status.getIr();
    }

    /**
     * Map containing all WeatherSensor Modules with their Data.
     *
     * @return Map of Modules with SensorData
     */
    public Map<Module, ClimateStatus> getAllClimateModuleStates() {
        return Collections.unmodifiableMap(climateStatusMapping);
    }

    ////Network/////////////////////////////////////////////////////////////////////////////////////

    /**
     * Sends Message to MasterClimateHandler to request SensorData of Module m.
     *
     * @param m Module to request data for
     */
    private void requestClimateStatus(Module m) {
        ClimateStatus status = climateStatusMapping.get(m);
        ClimatePayload climatePayload = new ClimatePayload(status.getTemp1(), status.getTemp2(),
                status.getPressure(), status.getAltitude(), status.getHumidity(), status.getUv(),
                status.getVisible(), status.getIr(), "", m);

        Message message = new Message(climatePayload);

        OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_REQUEST_WEATHER_INFO, message);
    }


    private void setClimate(ClimatePayload payload, String s) {
        ClimatePayload climatePayload = new ClimatePayload(payload, s);

        Message message;
        message = new Message(climatePayload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, APP_CLIMATE_UPDATE.getKey());

        OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_REQUEST_WEATHER_INFO, message);
    }

    //Lifecycle & Callbacks/////////////////////////////////////////////////////////////////////////

    /**
     * Handles received Message from MasterClimateHandler. Refreshes SensorData.
     *
     * @param message Message to handle.
     */
    @Override
    public void handle(Message.AddressedMessage message) {
        if (APP_CLIMATE_UPDATE.matches(message)) {
            ClimatePayload climatePayload = (ClimatePayload) message.getPayload();
            setCachedStatus(climatePayload.getModule(), climatePayload.getTemp1(), climatePayload.getTemp2(),
                    climatePayload.getPressure(), climatePayload.getAltitude(), climatePayload.getHumidity(),
                    climatePayload.getUv(), climatePayload.getIr(), climatePayload.getVisible());
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{APP_CLIMATE_UPDATE};
    }

    /**
     * Adds parameter handler to listeners.
     */
    public void addListener(ClimateHandlerListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes parameter handler from listeners.
     */
    public void removeListener(AppClimateHandler.ClimateHandlerListener listener) {
        listeners.remove(listener);
    }

    public interface ClimateHandlerListener {
        void statusChanged(Module module);
    }

    public class ClimateStatus {
        private double temp1;
        private double temp2;
        private double pressure;
        private double altitude;
        private double humidity;
        private double uv;
        private int visible;
        private int ir;
        private long timestamp;

        public ClimateStatus(double temp1, double temp2, double pressure, double altitude, double humidity,
                             double uv, int ir, int visible) {
            setStatus(temp1, temp2, pressure, altitude, humidity, uv, ir, visible);
        }

        /**
         * Sets the current data from the WeatherBoard.
         *
         * @param temp1    Temperature1
         * @param temp2    Temperature2
         * @param pressure AirPressure
         * @param altitude Altitude
         * @param humidity Humidity
         * @param uv       UV radiation
         * @param ir       IR radiation
         * @param visible  light intensity
         */
        public void setStatus(double temp1, double temp2, double pressure, double altitude, double humidity,
                              double uv, int ir, int visible) {
            this.temp1 = temp1;
            this.temp2 = temp2;
            this.pressure = pressure;
            this.altitude = altitude;
            this.humidity = humidity;
            this.uv = uv;
            this.ir = ir;
            this.visible = visible;
            this.timestamp = System.currentTimeMillis();
        }

        public double getTemp1() {
            return temp1;
        }

        public double getTemp2() {
            return temp2;
        }

        public double getPressure() {
            return pressure;
        }

        public double getAltitude() {
            return altitude;
        }

        public double getHumidity() {
            return humidity;
        }

        public double getUv() {
            return uv;
        }

        public int getIr() {
            return ir;
        }

        public int getVisible() {
            return visible;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

}
