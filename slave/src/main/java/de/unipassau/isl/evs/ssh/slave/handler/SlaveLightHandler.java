package de.unipassau.isl.evs.ssh.slave.handler;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.drivers.lib.EdimaxPlugSwitch;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

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

    private void handleSet(LightPayload payload, final Message.AddressedMessage original) {
        final Key<EdimaxPlugSwitch> key = new Key<>(EdimaxPlugSwitch.class, payload.getModule().getName());
        final EdimaxPlugSwitch plugSwitch = requireComponent(key);
        final boolean setOn = payload.getOn();

        plugSwitch.setOnAsync(setOn).addListener(new FutureListener<Boolean>() {
            @Override
            public void operationComplete(Future<Boolean> future) throws Exception {
                if (future.isSuccess()) {
                    if (future.get() == Boolean.TRUE) {
                        replyStatus(original, setOn);
                    } else {
                        sendReply(original, new Message(new ErrorPayload("Cannot switch light")));
                    }
                } else {
                    sendReply(original, new Message(new ErrorPayload(future.cause())));
                }
            }
        });
    }

    private void handleGet(LightPayload payload, final Message.AddressedMessage original) {
        final Key<EdimaxPlugSwitch> key = new Key<>(EdimaxPlugSwitch.class, payload.getModule().getName());
        requireComponent(key).isOnAsync().addListener(new FutureListener<Boolean>() {
            @Override
            public void operationComplete(Future<Boolean> future) throws Exception {
                if (future.isSuccess()) {
                    replyStatus(original, future.get());
                } else {
                    sendReply(original, new Message(new ErrorPayload(future.cause())));
                }
            }
        });
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