package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.WeatherPayload;
import de.unipassau.isl.evs.ssh.master.MasterConstants;

/**
 * Handles climate messages and generates messages for each target and passes them to the OutgoingRouter.
 *
 * @author Chris
 */
public class MasterClimateHandler extends AbstractMasterHandler {

    private boolean mainLampOn = false;

    @Override
    public void handle(Message.AddressedMessage message) {
        saveMessage(message);
        if (message.getPayload() instanceof WeatherPayload) {
            evaluateWeatherData(((WeatherPayload) message.getPayload()));
        } else if (message.getPayload() instanceof LightPayload) {
            LightPayload payload = (LightPayload) message.getPayload();
            mainLampOn = payload.getOn(); //TODO check if this is the first lamp
        }
    }

    private void evaluateWeatherData(WeatherPayload payload) {
        //The following values will not be checked as they are not of interest: Altitude, Pressure, Temp1, Temp2
        if (payload.getHumidity() > MasterConstants.ClimateThreshold.HUMIDITY) {
            WeatherPayload newPayload = new WeatherPayload(payload, CoreConstants.NotificationTypes.HUMIDITY_WARNING);
            sendMessageLocal(CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND, new Message(newPayload));
        }
        if (payload.getVisible() > MasterConstants.ClimateThreshold.VISIBLE_LIGHT && mainLampOn) {
            WeatherPayload newPayload = new WeatherPayload(payload, CoreConstants.NotificationTypes.BRIGHTNESS_WARNING);
            sendMessageLocal(CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND, new Message(newPayload));
        }
    }
}