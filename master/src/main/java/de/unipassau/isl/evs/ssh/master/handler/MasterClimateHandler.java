package de.unipassau.isl.evs.ssh.master.handler;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.core.sec.Permission;
import de.unipassau.isl.evs.ssh.master.MasterConstants;
import de.unipassau.isl.evs.ssh.master.network.NotificationBroadcaster;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_LIGHT_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_NOTIFICATION_SEND;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_PUSH_WEATHER_INFO;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_REQUEST_WEATHER_INFO;

/**
 * Handles climate messages and generates messages for each target and passes them to the OutgoingRouter.
 *
 * @author Christoph Fraedrich
 */
public class MasterClimateHandler extends AbstractMasterHandler {
    private final Map<Module, ClimatePayload> latestWeatherData = new HashMap<>();
    private final Map<Module, Boolean> latestLightStatus = new HashMap<>();

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_PUSH_WEATHER_INFO.matches(message)) {
            ClimatePayload payload = MASTER_PUSH_WEATHER_INFO.getPayload(message);
            latestWeatherData.put(payload.getModule(), payload) ;
            evaluateWeatherData(payload);
        } else if (MASTER_LIGHT_GET.matches(message) &&
                message.getHeader(Message.HEADER_REFERENCES_ID) != null) {
            //Reply to get request, this means this message actually contains an updated lamp value

            LightPayload payload = MASTER_LIGHT_GET.getPayload(message);

            latestLightStatus.put(payload.getModule(), payload.getOn());
        } else if (MASTER_REQUEST_WEATHER_INFO.matches(message)) {
            for(ClimatePayload payload : latestWeatherData.values()) {
                sendMessage(message.getFromID(), message.getHeader(Message.HEADER_REPLY_TO_KEY),
                        new Message(payload));
            }
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_PUSH_WEATHER_INFO, MASTER_LIGHT_GET, MASTER_REQUEST_WEATHER_INFO};
    }

    private void evaluateWeatherData(ClimatePayload payload) {
        NotificationBroadcaster notificationBroadcaster = new NotificationBroadcaster();
        //The following values will not be checked as they are not of interest: Altitude, Pressure, Temp1, Temp2
        if (payload.getHumidity() > MasterConstants.ClimateThreshold.HUMIDITY) {
            Serializable serializableHumidity = payload.getHumidity();
            notificationBroadcaster.sendMessageToAllReceivers(NotificationPayload.NotificationType.HUMIDITY_WARNING, serializableHumidity);
        }
        //TODO Schwellwert puffer hinzufügen. Wenn lampe eingeschaltet und über Schwellwert keine warnung.
        if (payload.getVisible() > MasterConstants.ClimateThreshold.VISIBLE_LIGHT) {
            for (Module module : latestLightStatus.keySet()) {
                if (latestLightStatus.get(module)) {
                    Serializable serializableLight = payload.getVisible();
                    notificationBroadcaster.sendMessageToAllReceivers(NotificationPayload.NotificationType.BRIGHTNESS_WARNING, serializableLight);
                }
            }
        }
    }
}