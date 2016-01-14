package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Log;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.network.broadcast.NotificationBroadcaster;

import static de.unipassau.isl.evs.ssh.core.messaging.Message.HEADER_REFERENCES_ID;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_DOOR_RING;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_CAMERA_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_CAMERA_GET_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_BELL_RING;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.BELL_RANG;

/**
 * Handles messages received when the doorbell is used, requests a picture from the camera
 * (by also sending a message to the MasterCameraHandler) and generates messages for each target
 * and passes them to the OutgoingRouter.
 *
 * @author Leon Sell
 */
public class MasterDoorBellHandler extends AbstractMasterHandler {
    private static final String TAG = MasterDoorBellHandler.class.getSimpleName();

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_DOOR_BELL_RING};
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_DOOR_BELL_RING.matches(message)) {
            handleDoorBellRing(message, MASTER_DOOR_BELL_RING.getPayload(message));
        } else {
            invalidMessage(message);
        }
    }



    private void handleDoorBellRing(Message.AddressedMessage message, DoorBellPayload doorBellPayload) {
        //Check if message comes from a slave.
        if (isSlave(message.getFromID())) {
            //Notify clients
            final NotificationBroadcaster notificationBroadcaster = requireComponent(NotificationBroadcaster.KEY);
            notificationBroadcaster.sendMessageToAllReceivers(
                    NotificationPayload.NotificationType.BELL_RANG,
                    doorBellPayload
            );

            //Get picture
            //Camera has to be the first camera of all added cameras. (database and android camera id)
            SlaveController slaveController = requireComponent(SlaveController.KEY);
            final Module camera = slaveController.getModulesByType(CoreConstants.ModuleType.Webcam).get(0);
            final Message messageToSend = new Message(new CameraPayload(0, camera.getName()));

            final Message.AddressedMessage sentMessage = sendMessageLocal(MASTER_CAMERA_GET, messageToSend);
            recordReceivedMessageProxy(message, sentMessage);
        } else {
            Log.e(TAG, "A non slave device tried to send a slave only message.");
        }
    }
}