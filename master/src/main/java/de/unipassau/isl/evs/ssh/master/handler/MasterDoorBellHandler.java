package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationWithPicturePayload;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

/**
 * Handles messages received when the doorbell is used, requests a picture from the camera
 * (by also sending a message to the MasterCameraHandler) and generates messages for each target
 * and passes them to the OutgoingRouter.
 */
public class MasterDoorBellHandler extends AbstractMasterHandler {
    private static final String DOOR_RANG_MESSAGE = ""; //Todo: write message

    @Override
    public void handle(Message.AddressedMessage message) {
        saveMessage(message);

        if (message.getPayload() instanceof DoorBellPayload) {
            DoorBellPayload doorBellPayload = (DoorBellPayload) message.getPayload();

            //Check permission
            if (true) { //Todo: check this message comes from a slave!
                Module atModule = incomingDispatcher.getContainer().require(SlaveController.KEY)
                        .getModule(doorBellPayload.getModuleName());

                Message messageToSend;
                //which functionality
                switch (message.getRoutingKey()) {
                    //Doorbell rings
                    case CoreConstants.RoutingKeys.MASTER_DOOR_BELL_RING:
                        messageToSend = new Message(new CameraPayload(0, "0")); //Todo: get corresponding camera!!!
                        messageToSend.putHeader(Message.HEADER_REPLY_TO_KEY,
                                CoreConstants.RoutingKeys.MASTER_DOOR_BELL_CAMERA_GET);

                        Message.AddressedMessage sendMessage = incomingDispatcher.getContainer()
                                .require(OutgoingRouter.KEY).sendMessageLocal(
                                        CoreConstants.RoutingKeys.MASTER_CAMERA_GET, messageToSend);
                        putOnBehalfOf(sendMessage.getSequenceNr(), message.getSequenceNr());
                        break;
                    default:
                        sendErrorMessage(message);
                        break;
                }
            } else {
                //no permission
                sendErrorMessage(message);
            }
        } else if (message.getPayload() instanceof CameraPayload) {
            CameraPayload cameraPayload = (CameraPayload) message.getPayload();

            //Check permission
            if (true) { //Todo: check this message comes from a slave!
                Message messageToSend;
                //which functionality
                switch (message.getRoutingKey()) {
                    //Requested picture arrives
                    case CoreConstants.RoutingKeys.MASTER_DOOR_BELL_CAMERA_GET:
                        //Todo check if has corresponding door ring
                        messageToSend = new Message(new NotificationWithPicturePayload(
                                CoreConstants.NotificationTypes.BELL_RANG, DOOR_RANG_MESSAGE, cameraPayload)); //Todo: add name of door to message.

                        incomingDispatcher.getContainer()
                                .require(OutgoingRouter.KEY).sendMessageLocal(
                                        CoreConstants.RoutingKeys.MASTER_NOTIFICATION_PICTURE_SEND, messageToSend);
                        break;
                    default:
                        sendErrorMessage(message);
                        break;
                }
            }

        } else if (message.getPayload() instanceof MessageErrorPayload) {
            //Todo: handle error
        } else {
            sendErrorMessage(message);
        }
    }
}