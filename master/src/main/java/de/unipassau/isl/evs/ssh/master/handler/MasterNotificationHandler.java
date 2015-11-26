package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;

/**
 * Handles notification messages and generates messages for each target and passes them to the OutgoingRouter.
 */
public class MasterNotificationHandler extends AbstractMasterHandler {

    public void handle(Message.AddressedMessage message) {
        saveMessage(message);

        if (message.getPayload() instanceof NotificationPayload) {
            NotificationPayload notificationPayload = (NotificationPayload) message.getPayload();
            Message messageToSend = new Message(notificationPayload);
            messageToSend.putHeader(Message.HEADER_REPLY_TO_KEY, message.getRoutingKey());
            messageToSend.putHeader(Message.HEADER_TIMESTAMP, System.currentTimeMillis());

            //which functionality
            switch (message.getRoutingKey()) {
                //Send notification
                case CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND:
                    for (UserDevice userDevice : incomingDispatcher.getContainer().require(PermissionController.KEY)
                            .getAllUserDevicesWithPermission(new Permission(notificationPayload.getMessage(), null))) {
                            incomingDispatcher.getContainer().require(OutgoingRouter.KEY)
                                    .sendMessage(userDevice.getUserDeviceID(),
                                            CoreConstants.RoutingKeys.MASTER_NOTIFICATION_RECEIVE, messageToSend);
                    }
                    break;
                default:
                    sendErrorMessage(message);
                    break;
            }
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            //Todo: handle error
        } else {
            sendErrorMessage(message);
        }
    }
}