package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

/**
 * Handles messages received when the doorbell is used, requests a picture from the camera
 * (by also sending a message to the MasterCameraHandler) and generates messages for each target
 * and passes them to the OutgoingRouter.
 */
public class MasterDoorBellHandler extends AbstractMasterHandler {

    @Override
    public void handle(Message.AddressedMessage message) {
        saveMessage(message);

        if (message.getPayload() instanceof DoorBellPayload) {

            //Check if message comes from a slave.
            if (isSlave(message.getFromID())) {

                //which functionality
                switch (message.getRoutingKey()) {
                    //Doorbell rings
                    case CoreConstants.RoutingKeys.MASTER_DOOR_BELL_RING:
                        handleDoorBellRing(message);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported routing key: " + message.getRoutingKey());
                }
            } else {
                //no permission
                sendErrorMessage(message);
            }
        } else if (message.getPayload() instanceof CameraPayload) {

            //Check if message comes from master
            if (requireComponent(NamingManager.KEY).getMasterID().equals(message.getFromID())) {

                //which functionality
                switch (message.getRoutingKey()) {
                    //Requested picture arrives
                    case CoreConstants.RoutingKeys.MASTER_DOOR_BELL_CAMERA_GET:
                        handleCameraResponse(message);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported routing key: " + message.getRoutingKey());
                }
            }

        } else if (message.getPayload() instanceof MessageErrorPayload) {
            handleErrorMessage(message);
        } else {
            sendErrorMessage(message);
        }
    }

    private void handleCameraResponse(Message.AddressedMessage message) {
        CameraPayload cameraPayload = (CameraPayload) message.getPayload();
        Message.AddressedMessage correspondingMessage = getMessageOnBehalfOfSequenceNr(message.getSequenceNr());
        DoorBellPayload doorBellPayload = (DoorBellPayload) correspondingMessage.getPayload();
        doorBellPayload.setCameraPayload(cameraPayload);

        Message messageToSend = new Message(doorBellPayload);

        for (UserDevice userDevice :
                requireComponent(PermissionController.KEY).getAllUserDevicesWithPermission(
                        new Permission(
                                CoreConstants.Permission.BinaryPermission.BELL_RANG.toString(),
                                null
                        )
                )) {
            sendMessage(userDevice.getUserDeviceID(), CoreConstants.RoutingKeys.APP_DOOR_RING, messageToSend);
        }
    }

    private void handleDoorBellRing(Message.AddressedMessage message) {
        //Camera has always to be the first camera of all added cameras. (database and id)
        /*
        Module camera = requireComponent(SlaveController.KEY).getModulesByType(CoreConstants.ModuleType.WEBCAM).get(0);
        Message messageToSend = new Message(new CameraPayload(0, camera.getName()));
        messageToSend.putHeader(Message.HEADER_REPLY_TO_KEY, CoreConstants.RoutingKeys.MASTER_DOOR_BELL_CAMERA_GET);
        */ //Todo: uncomment when camera is working

        Message.AddressedMessage sendMessage =
                //sendMessageLocal(
                //        CoreConstants.RoutingKeys.MASTER_CAMERA_GET,
                //        messageToSend
                //);
                sendMessageLocal(CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND, new Message(new NotificationPayload(CoreConstants.Permission.BinaryPermission.BELL_RANG.toString(), "Door bell rang.")));
        putOnBehalfOf(sendMessage.getSequenceNr(), message.getSequenceNr());
    }
}