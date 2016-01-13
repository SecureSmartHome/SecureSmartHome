package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

import static de.unipassau.isl.evs.ssh.core.messaging.Message.HEADER_REFERENCES_ID;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_CAMERA_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_CAMERA_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_CAMERA_GET_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_CAMERA_GET_REPLY;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.REQUEST_CAMERA_STATUS;

/**
 * Handles messages requesting pictures from the camera and generates messages, containing the pictures,
 * and sends these to the responsible NotificationBroadcaster.
 *
 * @author Leon Sell
 */
public class MasterCameraHandler extends AbstractMasterHandler {

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                MASTER_CAMERA_GET,
                SLAVE_CAMERA_GET_REPLY,
                SLAVE_CAMERA_GET_ERROR
        };
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_CAMERA_GET.matches(message)) {
            handleGetRequest(message, MASTER_CAMERA_GET.getPayload(message));
        } else if (SLAVE_CAMERA_GET_REPLY.matches(message)) {
            handleResponse(message, SLAVE_CAMERA_GET_REPLY.getPayload(message));
        } else if (SLAVE_CAMERA_GET_ERROR.matches(message)) {
            handleError(message, SLAVE_CAMERA_GET_ERROR.getPayload(message));
        } else {
            invalidMessage(message);
        }
    }

    private void handleError(Message.AddressedMessage message, ErrorPayload payload) {
        Message reply = new Message(payload);
        Message.AddressedMessage originalMessage = takeProxiedReceivedMessage(message.getHeader(HEADER_REFERENCES_ID));
        sendReply(originalMessage, reply);
    }

    private void handleGetRequest(Message.AddressedMessage message, CameraPayload cameraPayload) {
        Message messageToSend = new Message(cameraPayload);

        //Check permission
        if (hasPermission(message.getFromID(), REQUEST_CAMERA_STATUS)) {
            Module atModule = requireComponent(SlaveController.KEY).getModule(cameraPayload.getModuleName());
            Message.AddressedMessage sentMessage = sendMessage(atModule.getAtSlave(), SLAVE_CAMERA_GET, messageToSend);
            recordReceivedMessageProxy(message, sentMessage);
        } else {
            //no permission
            sendNoPermissionReply(message, REQUEST_CAMERA_STATUS);
        }
    }

    private void handleResponse(Message.AddressedMessage message, CameraPayload cameraPayload) {
        Message reply = new Message(cameraPayload);
        Message.AddressedMessage originalMessage = takeProxiedReceivedMessage(message.getHeader(HEADER_REFERENCES_ID));
        sendReply(originalMessage, reply);
    }
}