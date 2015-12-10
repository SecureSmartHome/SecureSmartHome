package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;

/**
 * Handles notification messages and generates messages for each target and passes them to the OutgoingRouter.
 *
 * @author bucher
 */
public class MasterNotificationHandler extends AbstractMasterHandler {

    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof NotificationPayload) {

            //Check if message is from master.
            if (requireComponent(NamingManager.KEY).getMasterID().equals(message.getFromID())) {
                //which functionality
                switch (message.getRoutingKey()) {
                    //Send notification
                    case CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND:
                        handleNotificationSend(message);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported routing key: " + message.getRoutingKey());
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

    private void handleNotificationSend(Message.AddressedMessage message) {
        NotificationPayload notificationPayload = (NotificationPayload) message.getPayload();
        Message messageToSend = new Message(notificationPayload);
        messageToSend.putHeader(Message.HEADER_REFERENCES_ID, message.getHeader(Message.HEADER_REFERENCES_ID));

        sendMessageToAllReceivers(
                messageToSend,
                notificationPayload.getType(),
                CoreConstants.RoutingKeys.APP_NOTIFICATION_RECEIVE
        );
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