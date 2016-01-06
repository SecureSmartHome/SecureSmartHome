package de.unipassau.isl.evs.ssh.slave.handler;

import android.util.Log;

import java.io.IOException;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.drivers.lib.EdimaxPlugSwitch;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_SET;

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
        if (SLAVE_LIGHT_SET.matches(message)) {
            handleSet(message);
        } else if (SLAVE_LIGHT_GET.matches(message)) {
            handleGet(message);
        } else {
            //Received wrong routing key -> invalid message
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{SLAVE_LIGHT_SET, SLAVE_LIGHT_GET};
    }

    private void handleSet(Message.AddressedMessage message) {
        final LightPayload payload = message.getPayloadChecked(LightPayload.class);
        final Key<EdimaxPlugSwitch> key = new Key<>(EdimaxPlugSwitch.class, payload.getModule().getName());
        final EdimaxPlugSwitch plugSwitch = requireComponent(key);

        boolean success = false;
        boolean isOn = payload.getOn();

        try {
            success = plugSwitch.setOn(isOn);
        } catch (IOException e) {
            Log.e(TAG, "Cannot switch lamp due to error", e);
        }

        if (success) {
            replyStatus(message, isOn);
        } else {
            //HANDLE
            sendErrorMessage(message);
        }
    }

    private void handleGet(Message.AddressedMessage message) {
        final LightPayload payload = message.getPayloadChecked(LightPayload.class);
        final Key<EdimaxPlugSwitch> key = new Key<>(EdimaxPlugSwitch.class, payload.getModule().getName());
        final EdimaxPlugSwitch plugSwitch = requireComponent(key);

        try {
            replyStatus(message, plugSwitch.isOn());
        } catch (IOException e) {
            Log.e(TAG, "Cannot retrieve lamp status due to error", e);
            //HANDLE
            sendErrorMessage(message);
        }
    }

    /**
     * Sends a reply containing an info whether the light is on or off
     *
     * @param original message that should get a reply
     */
    private void replyStatus(Message.AddressedMessage original, boolean isOn) {
        final LightPayload payload = original.getPayloadChecked(LightPayload.class);
        final Module module = payload.getModule();
        final Message reply = new Message(new LightPayload(isOn, module));
        reply.putHeader(Message.HEADER_REFERENCES_ID, original.getSequenceNr());
        sendMessage(original.getFromID(), original.getHeader(Message.HEADER_REPLY_TO_KEY), reply);
    }
}