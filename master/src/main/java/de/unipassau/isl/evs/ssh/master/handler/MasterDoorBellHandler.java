package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_DOOR_RING;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_CAMERA_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_BELL_CAMERA_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_BELL_RING;

/**
 * Handles messages received when the doorbell is used, requests a picture from the camera
 * (by also sending a message to the MasterCameraHandler) and generates messages for each target
 * and passes them to the OutgoingRouter.
 *
 * @author Leon Sell
 */
public class MasterDoorBellHandler extends AbstractMasterHandler {
    @Override
    public void handle(Message.AddressedMessage message) {
        saveMessage(message);

        if (MASTER_DOOR_BELL_RING.matches(message)) {
            handleDoorBellRing(message);
        } else if (MASTER_DOOR_BELL_CAMERA_GET.matches(message)) {
            handleCameraResponse(message);
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            handleErrorMessage(message);
        } else {
            sendErrorMessage(message);
        }
    }

    private void handleCameraResponse(Message.AddressedMessage message) {//Check if message comes from master
        if (requireComponent(NamingManager.KEY).getMasterID().equals(message.getFromID())) {
            CameraPayload cameraPayload = MASTER_DOOR_BELL_CAMERA_GET.getPayload(message);
            Message.AddressedMessage correspondingMessage = getMessageOnBehalfOfSequenceNr(message.getHeader(Message.HEADER_REFERENCES_ID));
            DoorBellPayload doorBellPayload = MASTER_DOOR_BELL_RING.getPayload(correspondingMessage);
            doorBellPayload.setCameraPayload(cameraPayload);

            Message messageToSend = new Message(doorBellPayload);

            for (UserDevice userDevice :
                    requireComponent(PermissionController.KEY).getAllUserDevicesWithPermission(
                            new Permission(
                                    de.unipassau.isl.evs.ssh.core.sec.Permission.BELL_RANG.toString(),
                                    null
                            )
                    )) {
                sendMessage(userDevice.getUserDeviceID(), APP_DOOR_RING, messageToSend);
            }
        } else {
            //no permission
            sendErrorMessage(message);
        }
    }

    private void handleDoorBellRing(Message.AddressedMessage message) {//Check if message comes from a slave.
        if (isSlave(message.getFromID())) {
            //Camera has always to be the first camera of all added cameras. (database and id)
            Module camera = requireComponent(SlaveController.KEY).getModulesByType(CoreConstants.ModuleType.Webcam).get(0);
            Message messageToSend = new Message(new CameraPayload(0, camera.getName()));
            messageToSend.putHeader(Message.HEADER_REPLY_TO_KEY, MASTER_DOOR_BELL_CAMERA_GET.getKey());

            Message.AddressedMessage sendMessage =
                    sendMessageLocal(
                            MASTER_CAMERA_GET,
                            messageToSend
                    );
            putOnBehalfOf(sendMessage.getSequenceNr(), message.getSequenceNr());
        } else {
            //no permission
            sendErrorMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_DOOR_BELL_RING, MASTER_DOOR_BELL_CAMERA_GET};
    }

}