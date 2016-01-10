package de.unipassau.isl.evs.ssh.slave.handler;

import android.util.Log;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorStatusPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.drivers.lib.DoorBuzzer;
import de.unipassau.isl.evs.ssh.drivers.lib.EvsIoException;
import de.unipassau.isl.evs.ssh.drivers.lib.ReedSensor;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_DOOR_STATUS_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_DOOR_UNLATCH;

/**
 * Handles door messages and makes API calls accordingly.
 *
 * @author Andreas Bucher
 * @author Wolfgang Popp
 */
public class SlaveDoorHandler extends AbstractMessageHandler {
    private static final String TAG = SlaveDoorHandler.class.getSimpleName();

    @Override
    public void handle(Message.AddressedMessage message) {
        if (SLAVE_DOOR_STATUS_GET.matches(message)) {
            handleDoorStatus(message);
        } else if (SLAVE_DOOR_UNLATCH.matches(message)) {
            handleUnlatchDoor(message);
            handleDoorStatus(message);
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{SLAVE_DOOR_STATUS_GET, SLAVE_DOOR_UNLATCH};
    }

    private void handleDoorStatus(Message.AddressedMessage original) {
        DoorStatusPayload incomingPayload = SLAVE_DOOR_STATUS_GET.getPayload(original);
        String moduleName = incomingPayload.getModuleName();
        Key<ReedSensor> key = new Key<>(ReedSensor.class, moduleName);
        ReedSensor doorSensor = requireComponent(key);

        try {
            final Message reply = new Message(new DoorStatusPayload(doorSensor.isOpen(), false, moduleName));
            sendReply(original, reply);
        } catch (EvsIoException e) {
            Log.e(TAG, "Cannot get door status", e);
            sendReply(original, new Message(new ErrorPayload(e)));
        }
    }

    private void handleUnlatchDoor(Message.AddressedMessage message) {
        DoorPayload payload = SLAVE_DOOR_UNLATCH.getPayload(message);
        Key<DoorBuzzer> key = new Key<>(DoorBuzzer.class, payload.getModuleName());
        DoorBuzzer doorBuzzer = requireComponent(key);
        try {
            doorBuzzer.unlock(3000);
            sendReply(message, new Message());
        } catch (EvsIoException e) {
            sendReply(message, new Message(new ErrorPayload(e)));
        }
    }
}