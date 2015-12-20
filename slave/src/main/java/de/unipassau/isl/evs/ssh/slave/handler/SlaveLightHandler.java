package de.unipassau.isl.evs.ssh.slave.handler;

import android.util.Log;

import java.io.IOException;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.drivers.lib.EdimaxPlugSwitch;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.SLAVE_LIGHT_GET;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.SLAVE_LIGHT_SET;

/**
 * Handles light messages and makes API calls accordingly.
 *
 * @author Christoph Fraedrich
 */
public class SlaveLightHandler extends AbstractMessageHandler {
    private static final String TAG = SlaveLightHandler.class.getSimpleName();

    /**
     * Will perform actions based on the message given, e.g. permission/sanity checks.
     *
     * @param message Message to handle.
     */
    @Override
    public void handle(Message.AddressedMessage message) {
        if (SLAVE_LIGHT_SET.matches(message) || SLAVE_LIGHT_GET.matches(message)) {
            final LightPayload payload = message.getPayloadChecked(LightPayload.class);
            final Key<EdimaxPlugSwitch> key = new Key<>(
                    EdimaxPlugSwitch.class,
                    payload.getModule().getName()
            );
            final EdimaxPlugSwitch plugSwitch = requireComponent(key);

            if (SLAVE_LIGHT_SET.matches(message)) {
                switchLight(message, plugSwitch);
            }
            replyStatus(message, plugSwitch);
        } else {
            //TODO check RoutingKey and call invalidMessage(message) otherwise
            final Message reply = new Message(new MessageErrorPayload(message.getPayload()));
            sendMessage(
                    message.getFromID(),
                    CoreConstants.RoutingKeys.MASTER_LIGHT_GET,
                    reply
            );
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{SLAVE_LIGHT_SET, SLAVE_LIGHT_GET};
    }

    /**
     * Method the hides switching the light on and off
     *
     * @param original   message that should get a reply
     * @param plugSwitch representing the driver of the lamp which is to be switched
     */
    private void switchLight(Message.AddressedMessage original, EdimaxPlugSwitch plugSwitch) {
        final LightPayload payload = SLAVE_LIGHT_SET.getPayload(original);
        try {
            if (payload.getOn() != plugSwitch.isOn()) {
                final boolean success = plugSwitch.setOn(payload.getOn());
                if (!success) {
                    Log.e(TAG, "Lamp did change status (should be " + payload.getOn() + ")");
                    sendErrorMessage(original);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Cannot switch lamp due to error", e);
            sendErrorMessage(original);
        }
    }

    /**
     * Sends a reply containing an info whether the light is on or off
     *
     * @param original message that should get a reply
     */
    private void replyStatus(Message.AddressedMessage original, EdimaxPlugSwitch plugSwitch) {
        final LightPayload payload = original.getPayloadChecked(LightPayload.class);
        final Module module = payload.getModule();
        try {
            final Message reply = new Message(new LightPayload(plugSwitch.isOn(), module));
            reply.putHeader(Message.HEADER_REFERENCES_ID, original.getSequenceNr());
            sendMessage(
                    original.getFromID(),
                    original.getHeader(Message.HEADER_REPLY_TO_KEY),
                    reply
            );
        } catch (IOException e) {
            Log.e(TAG, "Cannot retrieve lamp status due to error", e);
            sendErrorMessage(original);
        }
    }
}