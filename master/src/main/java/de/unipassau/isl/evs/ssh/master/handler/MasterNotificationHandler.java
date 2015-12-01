package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
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

            //Check if message is from master.
            if (requireComponent(NamingManager.KEY).getMasterID().equals(message.getFromID())) {
                //which functionality
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
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            handleErrorMessage(message);
        } else {
            sendErrorMessage(message);
        }
    }

    private void sendMessageToAllReceivers(Message messageToSend, String type, String routingKey) {
        for (UserDevice userDevice :
                requireComponent(PermissionController.KEY).getAllUserDevicesWithPermission(
                        new Permission(type, null)
                )) {
            sendMessage(userDevice.getUserDeviceID(), routingKey, messageToSend);
        }
    }
}