package de.unipassau.isl.evs.ssh.master.handler;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.master.MasterConstants;
import de.unipassau.isl.evs.ssh.master.network.NotificationBroadcaster;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_PUSH_WEATHER_INFO;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_REQUEST_WEATHER_INFO;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_REQUEST_WEATHER_INFO_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_GET_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.BRIGHTNESS_WARNING;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.HUMIDITY_WARNING;

/**
 * Handles climate messages and generates messages for each target and passes them to the OutgoingRouter.
 *
 * @author Christoph Fraedrich
 */
public class MasterClimateHandler extends AbstractMasterHandler implements Component {
    public static final Key<MasterClimateHandler> KEY = new Key<>(MasterClimateHandler.class);

    private static final long WARNING_TIMER = 5;

    private long humiditiyTimeStamp;
    private long brightnessTimeStamp;

    private final Map<Module, ClimatePayload> latestWeatherData = new HashMap<>();
    private final Map<Module, Boolean> latestLightStatus = new HashMap<>();

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_PUSH_WEATHER_INFO, SLAVE_LIGHT_GET_REPLY, MASTER_REQUEST_WEATHER_INFO};
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_PUSH_WEATHER_INFO.matches(message)) {
            ClimatePayload payload = MASTER_PUSH_WEATHER_INFO.getPayload(message);
            latestWeatherData.put(payload.getModule(), payload);
            evaluateWeatherData(payload);
        } else if (SLAVE_LIGHT_GET_REPLY.matches(message)) {
            //Reply to get request, this means this message actually contains an updated lamp value
            LightPayload payload = SLAVE_LIGHT_GET_REPLY.getPayload(message);
            latestLightStatus.put(payload.getModule(), payload.getOn());
        } else if (MASTER_REQUEST_WEATHER_INFO.matches(message)) {
            for (ClimatePayload payload : latestWeatherData.values()) {
                sendMessage(message.getFromID(), MASTER_REQUEST_WEATHER_INFO_REPLY, new Message(payload));
            }
        } else {
            invalidMessage(message);
        }
    }

    public Map<Module, ClimatePayload> getLatestWeatherData() {
        return new HashMap<>(latestWeatherData);
    }

    private void evaluateWeatherData(ClimatePayload payload) {
        evaluateHumidity(payload);
        evaluateBrightness(payload);
    }

    private void evaluateBrightness(ClimatePayload payload) {
        NotificationBroadcaster notificationBroadcaster = requireComponent(NotificationBroadcaster.KEY);
        //TODO Schwellwert puffer hinzufügen. Wenn lampe eingeschaltet und über Schwellwert keine warnung.
        if (payload.getVisible() > MasterConstants.ClimateThreshold.VISIBLE_LIGHT) {
            if (System.currentTimeMillis() - brightnessTimeStamp > TimeUnit.MINUTES.toMillis(WARNING_TIMER)) {
                for (Module module : latestLightStatus.keySet()) {
                    if (latestLightStatus.get(module)) {
                        Serializable serializableLight = payload.getVisible();
                        notificationBroadcaster.sendMessageToAllReceivers(BRIGHTNESS_WARNING, serializableLight);
                    }
                }

                brightnessTimeStamp = System.currentTimeMillis();
            }
        }
    }

    @NonNull
    private void evaluateHumidity(ClimatePayload payload) {
        NotificationBroadcaster notificationBroadcaster = requireComponent(NotificationBroadcaster.KEY);
        //The following values will not be checked as they are not of interest: Altitude, Pressure, Temp1, Temp2
        if (payload.getHumidity() > MasterConstants.ClimateThreshold.HUMIDITY) {
            if (System.currentTimeMillis() - humiditiyTimeStamp > TimeUnit.MINUTES.toMillis(WARNING_TIMER)) {
                Serializable serializableHumidity = payload.getHumidity();
                notificationBroadcaster.sendMessageToAllReceivers(HUMIDITY_WARNING, serializableHumidity);
                humiditiyTimeStamp= System.currentTimeMillis();
            }
        }
    }
}