package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.Permission;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.master.MasterConstants;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_LIGHT_GET;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_PUSH_WEATHER_INFO;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_REQUEST_WEATHER_INFO;

/**
 * Handles climate messages and generates messages for each target and passes them to the OutgoingRouter.
 *
 * @author Christoph Fraedrich
 */
public class MasterClimateHandler extends AbstractMasterHandler {
    private boolean mainLampOn = false;
    private ClimatePayload latestWeatherData;

    @Override
    public void handle(Message.AddressedMessage message) {
        saveMessage(message);
        if (MASTER_PUSH_WEATHER_INFO.matches(message)) {
            latestWeatherData = MASTER_PUSH_WEATHER_INFO.getPayload(message);
            evaluateWeatherData(latestWeatherData);
        } else if (MASTER_LIGHT_GET.matches(message)) {
            LightPayload payload = MASTER_LIGHT_GET.getPayload(message);
            mainLampOn = payload.getOn(); //TODO check if this is the first lamp
        } else if (MASTER_REQUEST_WEATHER_INFO.matches(message)) {
            //TODO make map of latestWeatherData to send data for each Weatherboard
            //Todo: request weather state
            if (latestWeatherData == null) {
                latestWeatherData = new ClimatePayload(0, 0, 0, 0, 0, 0, 0, 0, "", null);
            }
            sendMessage(message.getFromID(), message.getHeader(Message.HEADER_REPLY_TO_KEY),
                    new Message(latestWeatherData));
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_PUSH_WEATHER_INFO, MASTER_LIGHT_GET, MASTER_REQUEST_WEATHER_INFO};
    }

    private void evaluateWeatherData(ClimatePayload payload) {
        //The following values will not be checked as they are not of interest: Altitude, Pressure, Temp1, Temp2
        if (payload.getHumidity() > MasterConstants.ClimateThreshold.HUMIDITY) {
            ClimatePayload newPayload = new ClimatePayload(payload, Permission.HUMIDITY_WARNING.toString());
            sendMessageLocal(MASTER_NOTIFICATION_SEND, new Message(newPayload));
        }
        if (payload.getVisible() > MasterConstants.ClimateThreshold.VISIBLE_LIGHT && mainLampOn) {
            ClimatePayload newPayload = new ClimatePayload(payload, Permission.BRIGHTNESS_WARNING.toString());
            sendMessageLocal(MASTER_NOTIFICATION_SEND, new Message(newPayload));
        }
    }
}