package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_CAMERA_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_CAMERA_GET_REPLY;

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
            handleGetRequest(message);
        } else if (SLAVE_CAMERA_GET_REPLY.matches(message)) {
            handleResponse(message);
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            handleErrorMessage(message);
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_CAMERA_GET, SLAVE_CAMERA_GET_REPLY};
    }

    private void handleGetRequest(Message.AddressedMessage message) {
        CameraPayload cameraPayload = MASTER_CAMERA_GET.getPayload(message);
        Message messageToSend = new Message(cameraPayload);

        //Check permission
        if (hasPermission(
                message.getFromID(),
                de.unipassau.isl.evs.ssh.core.sec.Permission.REQUEST_CAMERA_STATUS,
                null
        )) {
            Module atModule = requireComponent(SlaveController.KEY).getModule(cameraPayload.getModuleName());
            Message.AddressedMessage sendMessage = sendMessage(
                    atModule.getAtSlave(),
                    RoutingKeys.SLAVE_CAMERA_GET, messageToSend
            );
            putOnBehalfOf(sendMessage.getSequenceNr(), message.getSequenceNr());
        } else {
            //no permission
            sendErrorMessage(message);
        }
    }

    private void handleResponse(Message.AddressedMessage message) {
        CameraPayload cameraPayload = SLAVE_CAMERA_GET_REPLY.getPayload(message);
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
}