package de.unipassau.isl.evs.ssh.app.handler;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.GPIOAccessPoint;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * AppClimateHandler class handles message from and to the
 * {@link de.unipassau.isl.evs.ssh.app.activity.ClimateFragment ClimateFragment}
 *
 * @author bucher
 */
public class AppClimateHandler extends AbstractComponent implements MessageHandler {
    public static final Key<AppClimateHandler> KEY = new Key<>(AppClimateHandler.class);
    private static final long REFRESH_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(1000);
    private final List<ClimateHandlerListener> listeners = new ArrayList<>();
    private final Map<Module, ClimateStatus> climateStatusMapping = new HashMap<>();

    public AppClimateHandler() {
        //create a TestWeatherBoard
        Module m = new Module("TestWeatherBoard", new DeviceID("H5f4ahpVmoVL6GKAYqZY7m73k9i9nDCnsiJLbw+0n3E="),
                CoreConstants.ModuleType.WEATHER_BOARD, new GPIOAccessPoint()); //FIXME resolve DeviceID
        climateStatusMapping.put(m, new ClimateStatus(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0));
    }

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
    private void setCachedStatus(Module module, double temp1, double temp2, double pressure, double altitude, double humidity,
                                 double uv, int ir, int visible) {
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

    //Getter for SensorData
    public double getTemp1(Module module) {
        final ClimateStatus status = climateStatusMapping.get(module);
        if (System.currentTimeMillis() - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            requestClimateStatus(module);
        }
        return status.getTemp1();
    }

    public double getTemp2(Module module) {
        final ClimateStatus status = climateStatusMapping.get(module);
        if (System.currentTimeMillis() - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            requestClimateStatus(module);
        }
        return status.getTemp2();
    }

    public double getPressure(Module module) {
        final ClimateStatus status = climateStatusMapping.get(module);
        if (System.currentTimeMillis() - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            requestClimateStatus(module);
        }
        return status.getPressure();
    }

    public double getAltitude(Module module) {
        final ClimateStatus status = climateStatusMapping.get(module);
        if (System.currentTimeMillis() - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            requestClimateStatus(module);
        }
        return status.getAltitude();
    }

    public double getHumidity(Module module) {
        final ClimateStatus status = climateStatusMapping.get(module);
        if (System.currentTimeMillis() - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            requestClimateStatus(module);
        }
        return status.getHumidity();
    }

    public double getUv(Module module) {
        final ClimateStatus status = climateStatusMapping.get(module);
        if (System.currentTimeMillis() - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            requestClimateStatus(module);
        }
        return status.getUv();
    }

    public int getVisible(Module module) {
        final ClimateStatus status = climateStatusMapping.get(module);
        if (System.currentTimeMillis() - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            requestClimateStatus(module);
        }
        return status.getVisible();
    }

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
        message.putHeader(Message.HEADER_REPLY_TO_KEY, CoreConstants.RoutingKeys.APP_CLIMATE_UPDATE);

        OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_REQUEST_WEATHER_INFO, message);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Registers the {@link IncomingDispatcher} as an component.
     */
    @Override
    public void init(Container container) {
        super.init(container);
        requireComponent(IncomingDispatcher.KEY).registerHandler(this, CoreConstants.RoutingKeys.APP_CLIMATE_UPDATE);
    }

    //Lifecycle & Callbacks/////////////////////////////////////////////////////////////////////////

    /**
     * Handles received Message from MasterClimateHandler. Refreshes SensorData.
     *
     * @param message Message to handle.
     */
    @Override
    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof ClimatePayload) {
            ClimatePayload climatePayload = (ClimatePayload) message.getPayload();
            setCachedStatus(climatePayload.getModule(), climatePayload.getTemp1(), climatePayload.getTemp2(), climatePayload.getPressure()
                    , climatePayload.getAltitude(), climatePayload.getHumidity(), climatePayload.getUv(), climatePayload.getIr(),
                    climatePayload.getVisible());
        } else {
            Log.e(this.getClass().getSimpleName(), "Error! Unknown message Payload");
        }
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
    }


    @Override
    public void handlerRemoved(String routingKey) {

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

    /**
     * Unregisters the {@link IncomingDispatcher} as an component.
     */
    @Override
    public void destroy() {
        requireComponent(IncomingDispatcher.KEY).unregisterHandler(this, CoreConstants.RoutingKeys.APP_CLIMATE_UPDATE);
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
