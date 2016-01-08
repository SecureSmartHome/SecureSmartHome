package de.unipassau.isl.evs.ssh.master.handler;

import java.util.HashMap;
import java.util.Map;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.sec.Permission;
import de.unipassau.isl.evs.ssh.master.MasterConstants;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_LIGHT_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_NOTIFICATION_SEND;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_PUSH_WEATHER_INFO;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_REQUEST_WEATHER_INFO;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_GET_REPLY;

/**
 * Handles climate messages and generates messages for each target and passes them to the OutgoingRouter.
 *
 * @author Christoph Fraedrich
 */
public class MasterClimateHandler extends AbstractMasterHandler implements Component {
    public static final Key<MasterClimateHandler> KEY = new Key<>(MasterClimateHandler.class);

    private final Map<Module, ClimatePayload> latestWeatherData = new HashMap<>();
    private final Map<Module, Boolean> latestLightStatus = new HashMap<>();

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_PUSH_WEATHER_INFO.matches(message)) {
            ClimatePayload payload = MASTER_PUSH_WEATHER_INFO.getPayload(message);
            latestWeatherData.put(payload.getModule(), payload) ;
            evaluateWeatherData(payload);
        } else if (SLAVE_LIGHT_GET_REPLY.matches(message)) {
            //Reply to get request, this means this message actually contains an updated lamp value
            LightPayload payload = SLAVE_LIGHT_GET_REPLY.getPayload(message);

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
        return new RoutingKey[]{MASTER_PUSH_WEATHER_INFO, SLAVE_LIGHT_GET_REPLY, MASTER_REQUEST_WEATHER_INFO};
    }

    public Map<Module, ClimatePayload> getLatestWeatherData() {
        return new HashMap<>(latestWeatherData);
    }

    private void evaluateWeatherData(ClimatePayload payload) {
        //The following values will not be checked as they are not of interest: Altitude, Pressure, Temp1, Temp2
        if (payload.getHumidity() > MasterConstants.ClimateThreshold.HUMIDITY) {
            ClimatePayload newPayload = new ClimatePayload(payload, Permission.HUMIDITY_WARNING.toString());
            sendMessageLocal(MASTER_NOTIFICATION_SEND, new Message(newPayload));
        }

        if(payload.getVisible() > MasterConstants.ClimateThreshold.VISIBLE_LIGHT )
        for (Module module : latestLightStatus.keySet()) {
            if (latestLightStatus.get(module)) {
                ClimatePayload newPayload = new ClimatePayload(payload, Permission.BRIGHTNESS_WARNING.toString());
                sendMessageLocal(MASTER_NOTIFICATION_SEND, new Message(newPayload));
            }
        }
    }
}