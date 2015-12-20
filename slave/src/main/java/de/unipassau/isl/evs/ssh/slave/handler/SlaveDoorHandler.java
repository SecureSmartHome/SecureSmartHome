package de.unipassau.isl.evs.ssh.slave.handler;

import android.util.Log;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorStatusPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorUnlatchPayload;
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
        DoorStatusPayload incomingPayload = (DoorStatusPayload) original.getPayload(); //FIXME ClassCastException for a SLAVE_DOOR_UNLATCH message DoorUnlatchPayload (Niko, 2015-12-19)
        String moduleName = incomingPayload.getModuleName();
        Key<ReedSensor> key = new Key<>(ReedSensor.class, moduleName);
        ReedSensor doorSensor = getContainer().require(key);

        try {
            final Message reply = new Message(new DoorStatusPayload(doorSensor.isOpen(), moduleName));
            reply.putHeader(Message.HEADER_REFERENCES_ID, original.getSequenceNr());
            sendMessage(original.getFromID(), original.getHeader(Message.HEADER_REPLY_TO_KEY), reply);
        } catch (EvsIoException e) {
            Log.e(TAG, "Cannot get door status", e);
            sendErrorMessage(original);
        }
    }

    private void handleUnlatchDoor(Message.AddressedMessage message) {
        DoorUnlatchPayload payload = SLAVE_DOOR_UNLATCH.getPayload(message);
        Key<DoorBuzzer> key = new Key<>(DoorBuzzer.class, payload.getModuleName());
        DoorBuzzer doorBuzzer = getContainer().require(key);
        try {
            doorBuzzer.unlock(3000);
        } catch (EvsIoException e) {
            Log.e(TAG, "Cannot unlock door", e);
            sendErrorMessage(message);
        }
    }
}