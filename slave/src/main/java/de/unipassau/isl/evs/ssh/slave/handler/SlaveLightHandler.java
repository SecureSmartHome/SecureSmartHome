package de.unipassau.isl.evs.ssh.slave.handler;

import android.util.Log;

import java.io.IOException;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.drivers.lib.EdimaxPlugSwitch;

/**
 * Handles light messages and makes API calls accordingly.
 *
 * @author Chris
 */
public class SlaveLightHandler extends AbstractSlaveHandler {
    /**
     * Will perform actions based on the message given, e.g. permission/sanity checks.
     *
     * @param message Message to handle.
     */
    @Override
    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof LightPayload) {
            final LightPayload payload = (LightPayload) message.getPayload();
            final Key<EdimaxPlugSwitch> key = new Key<>(
                    EdimaxPlugSwitch.class,
                    payload.getModule().getName()
            );
            final EdimaxPlugSwitch plugSwitch = requireComponent(key);

            switch (message.getRoutingKey()) {
                case CoreConstants.RoutingKeys.SLAVE_LIGHT_SET:
                    switchLight(message, plugSwitch);
                case CoreConstants.RoutingKeys.SLAVE_LIGHT_GET:
                    replyStatus(message, plugSwitch);
                    break;
            }
        } else {
            //TODO check Routing key
            final Message reply = new Message(new MessageErrorPayload(message.getPayload()));
            sendMessage(
                    message.getFromID(),
                    CoreConstants.RoutingKeys.MASTER_LIGHT_GET,
                    reply
            );
        }
    }

    /**
     * Method the hides switching the light on and off
     *
     * @param original   message that should get a reply
     * @param plugSwitch representing the driver of the lamp which is to be switched
     */
    private void switchLight(Message.AddressedMessage original, EdimaxPlugSwitch plugSwitch) {
        final LightPayload payload = (LightPayload) original.getPayload();
        try {
            if (payload.getOn() != plugSwitch.isOn()) {
                final boolean success = plugSwitch.setOn(payload.getOn());
                if (!success) {
                    Log.e(this.getClass().getSimpleName(), "Lamp did change status");
                    sendErrorMessage(original);
                }
            }
        } catch (IOException e) {
            Log.e(this.getClass().getSimpleName(), "Cannot switch lamp due to error", e);
            sendErrorMessage(original);
        }
    }

    /**
     * Sends a reply containing an info whether the light is on or off
     *
     * @param original message that should get a reply
     */
    private void replyStatus(Message.AddressedMessage original, EdimaxPlugSwitch plugSwitch) {
        final LightPayload payload = (LightPayload) original.getPayload();
        final Module module = payload.getModule();
        try {
            final Message reply = new Message(new LightPayload(plugSwitch.isOn(), module));
            reply.putHeader(Message.HEADER_REFERENCES_ID, original.getSequenceNr()); //TODO: getSequenzeNumber
            sendMessage(
                    original.getFromID(),
                    original.getHeader(Message.HEADER_REPLY_TO_KEY),
                    reply
            );
        } catch (IOException e) {
            Log.e(this.getClass().getSimpleName(), "Cannot retrieve lamp status due to error", e);
            sendErrorMessage(original);
        }
    }
}