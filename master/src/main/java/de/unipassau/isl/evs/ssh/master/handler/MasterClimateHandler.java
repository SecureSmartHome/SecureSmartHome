package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.WeatherPayload;
import de.unipassau.isl.evs.ssh.master.MasterConstants;

/**
 * Handles climate messages and generates messages for each target and passes them to the OutgoingRouter.
 */
public class MasterClimateHandler extends AbstractMasterHandler {

    @Override
    public void handle(Message.AddressedMessage message) {
        saveMessage(message);
        if (message.getPayload() instanceof WeatherPayload) {
            evaluateWeatherData(((WeatherPayload) message.getPayload()));
        }
    }

    private void evaluateWeatherData(WeatherPayload payload) {
        //The following values will not be checked as they are not of interest: Altitude
        if (payload.getHumidity() > MasterConstants.ClimateThreshold.HUMIDITY) {
            WeatherPayload newPayload = new WeatherPayload(payload, CoreConstants.NotificationTypes.HUMIDITY_WARNING);
        }
        if (payload.getPressure() > MasterConstants.ClimateThreshold.PRESSURE) {

        }
        if (payload.getTemp1() > MasterConstants.ClimateThreshold.TEMP1) {

        }
        if (payload.getTemp2() > MasterConstants.ClimateThreshold.TEMP2) {

        }
        if (payload.getVisible() > MasterConstants.ClimateThreshold.VISIBLE_LIGHT) {

        }
        if (payload.getAltitude() > MasterConstants.ClimateThreshold.ALTITUDE) {

        }
    }
}