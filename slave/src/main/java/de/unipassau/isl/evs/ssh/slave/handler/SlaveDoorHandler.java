package de.unipassau.isl.evs.ssh.slave.handler;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.drivers.lib.DoorBuzzer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_DOOR_UNLATCH;

/**
 * Handles door messages and makes API calls accordingly.
 *
 * @author Andreas Bucher
 * @author Wolfgang Popp
 */
public class SlaveDoorHandler extends AbstractMessageHandler {

    @Override
    public void handle(Message.AddressedMessage message) {
        if (SLAVE_DOOR_UNLATCH.matches(message)) {
            handleUnlatchDoor(SLAVE_DOOR_UNLATCH.getPayload(message), message);
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{SLAVE_DOOR_UNLATCH};
    }

    private void handleUnlatchDoor(final DoorPayload payload, final Message.AddressedMessage message) {
        Key<DoorBuzzer> key = new Key<>(DoorBuzzer.class, payload.getModuleName());
        requireComponent(key).unlock(8000).addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                if (future.isSuccess()) {
                    sendReply(message, new Message(new DoorPayload(payload.getModuleName())));
                } else {
                    sendReply(message, new Message(new ErrorPayload(future.cause())));
                }
            }
        });
    }
}