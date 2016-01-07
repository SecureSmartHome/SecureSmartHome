package de.unipassau.isl.evs.ssh.master.network;

import java.io.Serializable;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.ATTR_LOCAL_CONNECTION;

/**
 * Handles notification messages and generates messages for each target and passes them to the OutgoingRouter.
 *
 * @author Andreas Bucher
 */
public class NotificationBroadcaster extends AbstractComponent {

    public static Key<NotificationBroadcaster> KEY = new Key<>(NotificationBroadcaster.class);

    //TODO Check if everything's fine
    public void sendMessageToAllReceivers(NotificationPayload.NotificationType type, Serializable... args) {
        final List<UserDevice> allUserDevicesWithPermission = requireComponent(PermissionController.KEY)
                .getAllUserDevicesWithPermission(new Permission(type.getReceivePermission().name()));
        NotificationPayload payload = new NotificationPayload(type, args);
        Message messageToSend = new Message(payload);
        boolean userAtHome = false;
        //This might give an error as we do not
        // know if the enums and DTOs have the same names
        if (type.equals(NotificationPayload.NotificationType.WEATHER_WARNING)) {
            //If no one is at home everyone should get the WeatherWarning.
            //If someone with permission is at home, only them should get a notification.
            for (UserDevice userDevice : allUserDevicesWithPermission) {
                boolean isConnectionLocal = requireComponent(Server.KEY)
                        .findChannel(userDevice.getUserDeviceID())
                        .attr(ATTR_LOCAL_CONNECTION)
                        .get();
                if (isConnectionLocal) {
                    userAtHome = true;
                }
            }
            if (userAtHome) {
                for (UserDevice userDevice : allUserDevicesWithPermission) {
                    boolean isConnectionLocal = requireComponent(Server.KEY)
                            .findChannel(userDevice.getUserDeviceID())
                            .attr(ATTR_LOCAL_CONNECTION)
                            .get();
                    requireComponent(OutgoingRouter.KEY).sendMessage(userDevice.getUserDeviceID(),
                            RoutingKeys.APP_NOTIFICATION_RECEIVE, messageToSend);
                }
            } else {
                searchUsers(allUserDevicesWithPermission, messageToSend);
            }
        } else {
            searchUsers(allUserDevicesWithPermission, messageToSend);
        }
    }

    private void searchUsers(List<UserDevice> allUserDevicesWithPermission, Message messageToSend) {
        for (UserDevice userDevice : allUserDevicesWithPermission) {
            requireComponent(OutgoingRouter.KEY).sendMessage(userDevice.getUserDeviceID(),
                    RoutingKeys.APP_NOTIFICATION_RECEIVE, messageToSend);
        }
    }
}
