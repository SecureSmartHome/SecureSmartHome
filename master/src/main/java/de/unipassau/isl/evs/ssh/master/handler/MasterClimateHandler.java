package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.WeatherPayload;

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
        //How do I address the MasterNotificationHandler?
    }
}