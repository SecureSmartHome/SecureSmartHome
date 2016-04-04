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

package de.unipassau.isl.evs.ssh.app.handler;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_REQUEST_WEATHER_INFO_REPLY;

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
    private final AppModuleHandler.AppModuleListener moduleListener = new AppModuleHandler.AppModuleListener() {
        @Override
        public void onModulesRefreshed() {
            final List<Module> weather = requireComponent(AppModuleHandler.KEY).getWeather();
            climateStatusMapping.clear();
            for (Module module : weather) {
                climateStatusMapping.put(module, new ClimateStatus());
                refreshAllWeatherBoards();
            }
            fireStatusChanged();
        }
    };

    @Override
    public void init(Container container) {
        super.init(container);
        final AppModuleHandler moduleHandler = container.require(AppModuleHandler.KEY);
        for (Module module : moduleHandler.getWeather()) {
            climateStatusMapping.put(module, new ClimateStatus());
        }
        moduleHandler.addAppModuleListener(moduleListener);
    }

    @Override
    public void destroy() {
        final AppModuleHandler moduleHandler = getComponent(AppModuleHandler.KEY);
        if (moduleHandler != null) {
            moduleHandler.addAppModuleListener(moduleListener);
        }
        super.destroy();
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
        if (status != null) {
            status.setStatus(temp1, temp2, pressure, altitude, humidity, uv, ir, visible);
        }
    }

    /**
     * Requests the current climate status for a Module.
     * Stops the handler from pulling data more often than RefreshDelay allows it.
     *
     * @param module WeatherSensor Module.
     * @return current Climate data.
     */
    public ClimateStatus maybeRequestClimateStatus(Module module) {
        final ClimateStatus status = climateStatusMapping.get(module);
        final long now = System.currentTimeMillis();
        if (now - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            status.setTimestamp(now);
            requestClimateStatus(module);
        }
        return status;
    }

    /**
     * Return temperature that is measured in Temperature1 sensor.
     *
     * @param module The weatherboard which sensors are measured.
     * @return The temperature measured in Temperature1 sensor.
     */
    public double getTemp1(Module module) {
        return maybeRequestClimateStatus(module).getTemp1();
    }

    /**
     * Return temperature that is measured in Temperature2 sensor.
     *
     * @param module The weatherboard which sensors is measured.
     * @return The temperature measured in Temperature2 sensor.
     */
    public double getTemp2(Module module) {
        return maybeRequestClimateStatus(module).getTemp2();
    }

    /**
     * Return pressure that is measured in AirPressure sensor.
     *
     * @param module The weatherboard which sensor is measured.
     * @return The temperature measured in AirPressure sensor.
     */
    public double getPressure(Module module) {
        return maybeRequestClimateStatus(module).getPressure();
    }

    /**
     * Return altitude that is measured in AirPressure sensor.
     *
     * @param module The weatherboard which sensor is measured.
     * @return The temperature measured in Altitude sensor.
     */
    public double getAltitude(Module module) {
        return maybeRequestClimateStatus(module).getAltitude();
    }

    /**
     * Return humidity that is measured in Humidity sensor.
     *
     * @param module The weatherboard which sensor is measured.
     * @return The temperature measured in Humidity sensor.
     */
    public double getHumidity(Module module) {
        return maybeRequestClimateStatus(module).getHumidity();
    }

    /**
     * Return uv radiation that is measured in UV Radiation sensor.
     *
     * @param module The weatherboard which sensor is measured.
     * @return The temperature measured in UV Radiation sensor.
     */
    public double getUv(Module module) {
        return maybeRequestClimateStatus(module).getUv();
    }

    /**
     * Return ir radiation that is measured in IR Radiation sensor.
     *
     * @param module The weatherboard which sensor is measured.
     * @return The temperature measured in IR Radiation sensor.
     */
    public int getVisible(Module module) {
        return maybeRequestClimateStatus(module).getVisible();
    }

    /**
     * Return light intensity that is measured in Light Intensity sensor.
     *
     * @param module The weatherboard which sensor is measured.
     * @return The temperature measured in Light Intensity sensor.
     */
    public int getIr(Module module) {
        return maybeRequestClimateStatus(module).getIr();
    }

    /**
     * Map containing all WeatherSensor Modules with their Data.
     *
     * @return Map of Modules with SensorData
     */
    @NonNull
    public Map<Module, ClimateStatus> getAllClimateModuleStates() {
        return Collections.unmodifiableMap(climateStatusMapping);
    }

    /**
     * Sends Message to MasterClimateHandler to request SensorData of Module m.
     *
     * @param m Module to request data for
     */
    private void requestClimateStatus(Module m) {
        ClimateStatus status = climateStatusMapping.get(m);
        ClimatePayload climatePayload;
        if (status != null) {
            climatePayload = new ClimatePayload(status.getTemp1(), status.getTemp2(),
                    status.getPressure(), status.getAltitude(), status.getHumidity(), status.getUv(),
                    status.getVisible(), status.getIr(), m);
        } else {
            climatePayload = new ClimatePayload(0, 0, 0, 0, 0, 0, 0, 0, m);
        }
        sendMessageToMaster(RoutingKeys.MASTER_REQUEST_WEATHER_INFO, new Message(climatePayload));
    }

    /**
     * Sends Message to MasterClimateHandler to request SensorData of every Module.
     */
    public void refreshAllWeatherBoards() {
        for (Module module : climateStatusMapping.keySet()) {
            requestClimateStatus(module);
        }
    }

    /**
     * Handles received Message from MasterClimateHandler. Refreshes SensorData.
     *
     * @param message Message to handle.
     */
    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_REQUEST_WEATHER_INFO_REPLY.matches(message)) {
            ClimatePayload climatePayload = MASTER_REQUEST_WEATHER_INFO_REPLY.getPayload(message);
            setCachedStatus(climatePayload.getModule(), climatePayload.getTemp1(), climatePayload.getTemp2(),
                    climatePayload.getPressure(), climatePayload.getAltitude(), climatePayload.getHumidity(),
                    climatePayload.getUv(), climatePayload.getIr(), climatePayload.getVisible());
            fireStatusChanged();
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_REQUEST_WEATHER_INFO_REPLY};
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

    private void fireStatusChanged() {
        for (ClimateHandlerListener listener : listeners) {
            listener.statusChanged();
        }
    }

    /**
     * Checks if there are weatherBoards registered in the system and puts all of them in the
     * climate mapping.
     * They all get their default values.
     */
    public void maybeUpdateModules() {
        List<Module> weatherBoards = requireComponent(AppModuleHandler.KEY).getWeather();
        if (weatherBoards.size() > climateStatusMapping.keySet().size()) {
            for (Module weatherBoard : weatherBoards) {
                if (!climateStatusMapping.containsKey(weatherBoard)) {
                    climateStatusMapping.put(weatherBoard, new ClimateStatus());
                }
            }
        }
    }

    public interface ClimateHandlerListener {
        void statusChanged();
    }

    /**
     * ClimateStatus saves the current climateData for a module.
     */
    private class ClimateStatus {
        private double temp1;
        private double temp2;
        private double pressure;
        private double altitude;
        private double humidity;
        private double uv;
        private int visible;
        private int ir;
        private long timestamp;

        private ClimateStatus() {
            setStatus(0, 0, 0, 0, 0, 0, 0, 0);
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

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
