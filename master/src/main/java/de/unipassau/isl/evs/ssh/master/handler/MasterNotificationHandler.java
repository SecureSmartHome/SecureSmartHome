package de.unipassau.isl.evs.ssh.master.handler;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_NOTIFICATION_RECEIVE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_NOTIFICATION_SEND;

/**
 * Handles notification messages and generates messages for each target and passes them to the OutgoingRouter.
 *
 * @author Andreas Bucher
 */
public class MasterNotificationHandler extends AbstractMasterHandler {

    public void handle(Message.AddressedMessage message) {
        if (MASTER_NOTIFICATION_SEND.matches(message)) {
            //Check if message is from master.
            if (requireComponent(NamingManager.KEY).getMasterID().equals(message.getFromID())) {
                //Send notification
                handleNotificationSend(message);
            } else {
                sendErrorMessage(message);
            }
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            handleErrorMessage(message);
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_NOTIFICATION_SEND};
    }

    private void handleNotificationSend(Message.AddressedMessage message) {
        NotificationPayload notificationPayload = MASTER_NOTIFICATION_SEND.getPayload(message);
        Message messageToSend = new Message(notificationPayload);
        messageToSend.putHeader(Message.HEADER_REFERENCES_ID, message.getHeader(Message.HEADER_REFERENCES_ID));

        sendMessageToAllReceivers(
                messageToSend,
                de.unipassau.isl.evs.ssh.core.sec.Permission.valueOf(notificationPayload.getType()),
                APP_NOTIFICATION_RECEIVE
        );
    }

    private void sendMessageToAllReceivers(Message messageToSend,
                                           de.unipassau.isl.evs.ssh.core.sec.Permission permission,
                                           RoutingKey routingKey
    ) {
        final List<UserDevice> allUserDevicesWithPermission = requireComponent(PermissionController.KEY)
                .getAllUserDevicesWithPermission(permission, null);
        for (UserDevice userDevice : allUserDevicesWithPermission) {
            sendMessage(userDevice.getUserDeviceID(), routingKey, messageToSend);
        }
    }
}