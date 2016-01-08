package de.unipassau.isl.evs.ssh.slave.handler;

import java.io.IOException;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.drivers.lib.EdimaxPlugSwitch;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_SET;

/**
 * Handles light messages and makes API calls accordingly.
 *
 * @author Christoph Fraedrich
 * @author Wolfgang Popp
 */
public class SlaveLightHandler extends AbstractMessageHandler {

    /**
     * Will perform actions based on the message given, e.g. permission/sanity checks.
     *
     * @param message Message to handle.
     */
    @Override
    public void handle(Message.AddressedMessage message) {
        if (SLAVE_LIGHT_SET.matches(message)) {
            handleSet(SLAVE_LIGHT_SET.getPayload(message), message);
        } else if (SLAVE_LIGHT_GET.matches(message)) {
            handleGet(SLAVE_LIGHT_GET.getPayload(message), message);
        } else {
            //Received wrong routing key -> invalid message
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{SLAVE_LIGHT_SET, SLAVE_LIGHT_GET};
    }

    private void handleSet(LightPayload payload, Message.AddressedMessage original) {
        final Key<EdimaxPlugSwitch> key = new Key<>(EdimaxPlugSwitch.class, payload.getModule().getName());
        final EdimaxPlugSwitch plugSwitch = requireComponent(key);

        boolean isOn = payload.getOn();
        boolean success;

        try {
            success = plugSwitch.setOn(isOn);
        } catch (IOException e) {
            success = false;
        }

        if (success) {
            replyStatus(original, isOn);
        } else {
            sendReply(original, new Message(new ErrorPayload("Cannot switch light")));
        }

    }

    private void handleGet(LightPayload payload, Message.AddressedMessage original) {
        final Key<EdimaxPlugSwitch> key = new Key<>(EdimaxPlugSwitch.class, payload.getModule().getName());
        final EdimaxPlugSwitch plugSwitch = requireComponent(key);

        try {
            replyStatus(original, plugSwitch.isOn());
        } catch (IOException e) {
            sendReply(original, new Message(new ErrorPayload(e)));
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
        sendReply(original, reply);
    }
}