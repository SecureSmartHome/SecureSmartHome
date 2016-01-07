package de.unipassau.isl.evs.ssh.master.network;

import java.io.Serializable;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;

/**
 * Handles notification messages and generates messages for each target and passes them to the OutgoingRouter.
 *
 * @author Andreas Bucher
 */
public class NotificationBroadcaster implements Component {

    public static Key<NotificationBroadcaster> KEY = new Key<>(NotificationBroadcaster.class) ;
    private Container container;

    //TODO Check if everythings fine
    public void sendMessageToAllReceivers(NotificationPayload.NotificationType type, Serializable... args) {
        final List<UserDevice> allUserDevicesWithPermission = container.require(PermissionController.KEY)
                .getAllUserDevicesWithPermission(new Permission(type.getReceivePermission().name())); //This might give an error as we do not
                                                                                                      // know if the enums and DTOs have the same names

        NotificationPayload payload = new NotificationPayload(type, args);
        Message messageToSend = new Message(payload);
        for (UserDevice userDevice : allUserDevicesWithPermission) {
            container.require(OutgoingRouter.KEY).sendMessage(userDevice.getUserDeviceID(), RoutingKeys.APP_NOTIFICATION_RECEIVE, messageToSend);
        }
    }

    @Override
    public void init(Container container) {
        this.container = container;
    }

    @Override
    public void destroy() {
    }
}