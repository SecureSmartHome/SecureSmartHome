package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationWithPicturePayload;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;

/**
 * Handles notification messages and generates messages for each target and passes them to the OutgoingRouter.
 */
public class MasterNotificationHandler extends AbstractMasterHandler {

    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof NotificationPayload) {
            NotificationPayload notificationPayload = (NotificationPayload) message.getPayload();
            Message messageToSend = new Message(notificationPayload);
            messageToSend.putHeader(Message.HEADER_REFERENCES_ID, message.getHeader(Message.HEADER_REFERENCES_ID));

            //which functionality
            if (true) { //Todo: check permission
                switch (message.getRoutingKey()) {
                    //Send notification
                    case CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND:
                        sendMessageToAllReceivers(messageToSend, notificationPayload.getType(),
                                CoreConstants.RoutingKeys.APP_NOTIFICATION_RECEIVE);
                        break;
                    default:
                        sendErrorMessage(message);
                        break;
                }
            } else {
                sendErrorMessage(message);
            }
        } else if (message.getPayload() instanceof NotificationWithPicturePayload) {
            NotificationWithPicturePayload notificationWithPicturePayload =
                    (NotificationWithPicturePayload) message.getPayload();
            Message messageToSend = new Message(notificationWithPicturePayload);
            messageToSend.putHeader(Message.HEADER_REFERENCES_ID, message.getHeader(Message.HEADER_REFERENCES_ID));

            //which functionality
            if (true) { //Todo: check permission
                switch (message.getRoutingKey()) {
                    //Send notification with picture
                    case CoreConstants.RoutingKeys.MASTER_NOTIFICATION_PICTURE_SEND:
                        sendMessageToAllReceivers(messageToSend, notificationWithPicturePayload.getType(),
                                CoreConstants.RoutingKeys.APP_NOTIFICATION_PICTURE_RECEIVE);
                        break;
                    default:
                        sendErrorMessage(message);
                        break;
                }
            } else {
                sendErrorMessage(message);
            }

        } else if (message.getPayload() instanceof MessageErrorPayload) {
            //Todo: handle error
        } else {
            sendErrorMessage(message);
        }
    }

    private void sendMessageToAllReceivers(Message messageToSend, String type, String routingKey) {
        for (UserDevice userDevice : incomingDispatcher.getContainer().require(PermissionController.KEY)
                .getAllUserDevicesWithPermission(new Permission(type, null))) {
            incomingDispatcher.getContainer().require(OutgoingRouter.KEY)
                    .sendMessage(userDevice.getUserDeviceID(), routingKey, messageToSend);
        }
    }
}