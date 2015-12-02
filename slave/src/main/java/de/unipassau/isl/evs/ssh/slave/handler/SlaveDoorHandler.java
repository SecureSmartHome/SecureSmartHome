package de.unipassau.isl.evs.ssh.slave.handler;

import android.util.Log;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorStatusPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorUnlatchPayload;
import de.unipassau.isl.evs.ssh.drivers.lib.DoorBuzzer;
import de.unipassau.isl.evs.ssh.drivers.lib.EvsIoException;
import de.unipassau.isl.evs.ssh.drivers.lib.ReedSensor;

/**
 * Handles door messages and makes API calls accordingly.
 *
 * @author bucher
 */
public class SlaveDoorHandler extends AbstractSlaveHandler {
    private static final String TAG = SlaveDoorHandler.class.getSimpleName();

    @Override
    public void handle(Message.AddressedMessage message) {
        String routingKey = message.getRoutingKey();
        if (routingKey.equals(CoreConstants.RoutingKeys.SLAVE_DOOR_STATUS_GET)) {
            handleDoorStatus(message);
        } else if (routingKey.equals(CoreConstants.RoutingKeys.SLAVE_DOOR_UNLATCH)) {
            handleUnlatchDoor(message);
            handleDoorStatus(message);
        }
    }

    private void handleDoorStatus(Message.AddressedMessage original) {
        DoorStatusPayload incomingPayload = (DoorStatusPayload) original.getPayload();
        String moduleName = incomingPayload.getModuleName();
        Key<ReedSensor> key = new Key<>(ReedSensor.class, moduleName);
        ReedSensor doorSensor = getContainer().require(key);
        boolean isDoorOpen = false;

        try {
            isDoorOpen = doorSensor.isOpen();
        } catch (EvsIoException e) {
            Log.e(TAG, "Cannot get door status", e);
            sendErrorMessage(original);
            return;
        }

        final Message reply = new Message(new DoorStatusPayload(isDoorOpen, moduleName));
        reply.putHeader(Message.HEADER_REFERENCES_ID, original.getSequenceNr());
        sendMessage(original.getFromID(), original.getHeader(Message.HEADER_REPLY_TO_KEY), reply);
    }

    private void handleUnlatchDoor(Message.AddressedMessage message) {
        DoorUnlatchPayload payload = (DoorUnlatchPayload) message.getPayload();
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