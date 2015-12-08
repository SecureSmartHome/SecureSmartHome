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
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.WLANAccessPoint;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * AppLightHandler class handles message from and to the
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
        Module m = new Module("TestWeatherBoard", new DeviceID("H5f4ahpVmoVL6GKAYqZY7m73k9i9nDCnsiJLbw+0n3E="),
                 CoreConstants.ModuleType.WEATHER_BOARD, new GPIOAccessPoint()); //FIXME resolve DeviceID
        climateStatusMapping.put(m, new ClimateStatus(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0));
    }

    public void toggleClimate(ClimatePayload payload, String message) {
        setClimate(payload, message);
    }

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

    //TODO ClimateStatus
    public boolean isLightOn(ClimatePayload payload) {
        final ClimateStatus status = climateStatusMapping.get(payload);
        if (System.currentTimeMillis() - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            requestClimateStatus(payload);
        }
        return isClimateOnCached(false);
    }

    private boolean isClimateOnCached(boolean b) {
        return false;
    }

    public Map<Module, ClimateStatus> getAllClimateModuleStates() {
        return Collections.unmodifiableMap(climateStatusMapping);
    }

    ////Network/////////////////////////////////////////////////////////////////////////////////////

    private void requestClimateStatus(ClimatePayload payload) {
        ClimatePayload climatePayload = new ClimatePayload(payload, "");

        Message message = new Message(climatePayload);

        OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_LIGHT_GET, message);
    }

    private void setClimate(ClimatePayload payload,  String s) {
        ClimatePayload climatePayload= new ClimatePayload(payload, s);

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

    @Override
    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof ClimatePayload) {
            ClimatePayload climatePayload= (ClimatePayload) message.getPayload();
            setCachedStatus( climatePayload.getModule(), climatePayload.getTemp1(), climatePayload.getTemp2(), climatePayload.getPressure()
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
