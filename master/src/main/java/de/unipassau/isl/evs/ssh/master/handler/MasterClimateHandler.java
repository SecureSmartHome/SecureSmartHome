package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;
import de.unipassau.isl.evs.ssh.master.MasterConstants;

/**
 * Handles climate messages and generates messages for each target and passes them to the OutgoingRouter.
 *
 * @author Chris
 */
public class MasterClimateHandler extends AbstractMasterHandler {

    private boolean mainLampOn = false;
    private ClimatePayload latestWeatherData;

    @Override
    public void handle(Message.AddressedMessage message) {
        saveMessage(message);
        if (CoreConstants.RoutingKeys.MASTER_PUSH_WEATHER_INFO.equals(message.getRoutingKey())) {
            latestWeatherData = (ClimatePayload) message.getPayload();
            evaluateWeatherData(((ClimatePayload) message.getPayload()));
        } else if (CoreConstants.RoutingKeys.MASTER_LIGHT_GET.equals(message.getRoutingKey())) {
            LightPayload payload = (LightPayload) message.getPayload();
            mainLampOn = payload.getOn(); //TODO check if this is the first lamp
        } else if (CoreConstants.RoutingKeys.MASTER_REQUEST_WEATHER_INFO.equals(message.getRoutingKey())) {
            //TODO make map of latestWeatherData to send data for each Weatherboard
            if (latestWeatherData == null) {
                latestWeatherData = new ClimatePayload(0,0,0,0,0,0,0,0,"", "");
            }
            sendMessage(message.getFromID(), message.getHeader(Message.HEADER_REPLY_TO_KEY),
                    new Message(latestWeatherData));
        }
    }

    private void evaluateWeatherData(ClimatePayload payload) {
        //The following values will not be checked as they are not of interest: Altitude, Pressure, Temp1, Temp2
        if (payload.getHumidity() > MasterConstants.ClimateThreshold.HUMIDITY) {
            ClimatePayload newPayload = new ClimatePayload(payload, CoreConstants.NotificationTypes.HUMIDITY_WARNING);
            sendMessageLocal(CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND, new Message(newPayload));
        }
        if (payload.getVisible() > MasterConstants.ClimateThreshold.VISIBLE_LIGHT && mainLampOn) {
            ClimatePayload newPayload = new ClimatePayload(payload, CoreConstants.NotificationTypes.BRIGHTNESS_WARNING);
            sendMessageLocal(CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND, new Message(newPayload));
        }
    }
}