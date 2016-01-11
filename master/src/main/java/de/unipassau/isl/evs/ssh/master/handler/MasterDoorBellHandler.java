package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.network.NotificationBroadcaster;

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
    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_DOOR_BELL_RING, MASTER_CAMERA_GET_REPLY};
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_DOOR_BELL_RING.matches(message)) {
            handleDoorBellRing(message, MASTER_DOOR_BELL_RING.getPayload(message));
        } else if (MASTER_CAMERA_GET_REPLY.matches(message)) {
            handleCameraResponse(message, MASTER_CAMERA_GET_REPLY.getPayload(message));
        } else {
            invalidMessage(message);
        }
    }

    private void handleCameraResponse(Message.AddressedMessage message, CameraPayload cameraPayload) {
        //Check if message comes from master
        if (isMaster(message.getFromID())) {
            final Message.AddressedMessage correspondingMessage =
                    takeProxiedReceivedMessage(message.getHeader(HEADER_REFERENCES_ID));
            final DoorBellPayload doorBellPayload = MASTER_DOOR_BELL_RING.getPayload(correspondingMessage);
            doorBellPayload.setCameraPayload(cameraPayload);

            final Message messageToSend = new Message(doorBellPayload);

            sendMessageToAllDevicesWithPermission(messageToSend, BELL_RANG, null, APP_DOOR_RING);
        } else {
            //no permission
            sendReply(message, new Message(new ErrorPayload("Camera replies can only be sent from the master.")));
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
            //no permission
            sendReply(message, new Message(new ErrorPayload("Door bell ring message can only be sent from a slave.")));
        }
    }
}