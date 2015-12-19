package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_CAMERA_GET;

/**
 * Handles messages requesting pictures from the camera and generates messages, containing the pictures,
 * and sends these to the responsible MasterNotificationHandler.
 *
 * @author Leon Sell
 */
public class MasterCameraHandler extends AbstractMasterHandler {

    @Override
    public void handle(Message.AddressedMessage message) {
        saveMessage(message);
        if (MASTER_CAMERA_GET.matches(message)) {
            if (message.getHeader(Message.HEADER_REFERENCES_ID) == null) {
                handleGetRequest(message);
            } else {
                //Response
                handleResponse(message);
            }
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            handleErrorMessage(message);
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_CAMERA_GET};
    }

    private void handleResponse(Message.AddressedMessage message) {
        CameraPayload cameraPayload = MASTER_CAMERA_GET.getPayload(message);
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
        CameraPayload cameraPayload = MASTER_CAMERA_GET.getPayload(message);
        Module atModule = requireComponent(SlaveController.KEY).getModule(cameraPayload.getModuleName());
        Message messageToSend = new Message(cameraPayload);
        messageToSend.putHeader(Message.HEADER_REPLY_TO_KEY, message.getRoutingKey());

        //Check permission
        if (hasPermission(
                message.getFromID(),
                new Permission(
                        CoreConstants.Permission.BinaryPermission.REQUEST_CAMERA_STATUS.toString(),
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