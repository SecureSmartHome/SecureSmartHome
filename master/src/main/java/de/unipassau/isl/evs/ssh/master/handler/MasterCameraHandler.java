package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.master.database.DatabaseContract;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

/**
 * Handles messages requesting pictures from the camera and generates messages, containing the pictures,
 * and sends these to the responsible MasterNotificationHandler.
 */
public class MasterCameraHandler extends AbstractMasterHandler {

    @Override
    public void handle(Message.AddressedMessage message) {
        saveMessage(message);

        if (message.getPayload() instanceof CameraPayload) {
            //Response or request?
            if (message.getHeader(Message.HEADER_REFERENCES_ID) == null) {
                //Request
                //which functionality
                switch (message.getRoutingKey()) {
                    //Get status
                    case CoreConstants.RoutingKeys.MASTER_CAMERA_GET:
                        handleGetRequest(message);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported routing key: " + message.getRoutingKey());
                }
            } else {
                //Response
                handleResponse(message);
            }
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            handleErrorMessage(message);
        } else {
            sendErrorMessage(message);
        }
    }

    private void handleResponse(Message.AddressedMessage message) {
        CameraPayload cameraPayload = (CameraPayload) message.getPayload();
        Message.AddressedMessage correspondingMessage =
                getMessageOnBehalfOfSequenceNr(message.getHeader(Message.HEADER_REFERENCES_ID));
        Message messageToSend = new Message(cameraPayload);
        messageToSend.putHeader(Message.HEADER_REFERENCES_ID, correspondingMessage.getSequenceNr());

        sendMessage(
                correspondingMessage.getFromID(),
                correspondingMessage.getHeader(Message.HEADER_REPLY_TO_KEY),
                messageToSend
        );
    }

    private void handleGetRequest(Message.AddressedMessage message) {
        CameraPayload cameraPayload = (CameraPayload) message.getPayload();
        Module atModule = requireComponent(SlaveController.KEY).getModule(cameraPayload.getModuleName());
        Message messageToSend = new Message(cameraPayload);
        messageToSend.putHeader(Message.HEADER_REPLY_TO_KEY, message.getRoutingKey());

        //Check permission
        if (hasPermission(
                message.getFromID(),
                new Permission(
                        DatabaseContract.Permission.Values.REQUEST_CAMERA_STATUS,
                        atModule.getName()
                )
        )) {
            Message.AddressedMessage sendMessage =
                    sendMessage(
                            atModule.getAtSlave(),
                            CoreConstants.RoutingKeys.SLAVE_CAMERA_GET, messageToSend
                    );
            putOnBehalfOf(sendMessage.getSequenceNr(), message.getSequenceNr());
        } else {
            //no permission
            sendErrorMessage(message);
        }
    }
}