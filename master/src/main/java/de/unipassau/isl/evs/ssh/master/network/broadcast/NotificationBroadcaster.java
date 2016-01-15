package de.unipassau.isl.evs.ssh.master.network.broadcast;

import android.util.Log;

import java.io.Serializable;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.handler.MasterUserLocationHandler;
import de.unipassau.isl.evs.ssh.master.network.Server;
import io.netty.channel.Channel;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.ATTR_LOCAL_CONNECTION;

/**
 * Handles notification messages and generates messages for each target and passes them to the OutgoingRouter.
 *
 * @author Andreas Bucher
 */
public class NotificationBroadcaster extends AbstractComponent {
    public static final Key<NotificationBroadcaster> KEY = new Key<>(NotificationBroadcaster.class);
    private static final String TAG = NotificationBroadcaster.class.getSimpleName();

    /**
     * Sends out a notification to all users that have the permission to receive that notification.
     * If the notification is a WEATHER_WARNING or HUMIDITY_WARNING it first checks for users that
     * are at home. If no one is at home, everyone with permission will receive the notification.
     *
     * @param type of the notification e.g. WEATHER_WARNING
     * @param args data that should be displayed in the notification e.g. humidity
     */
    public void sendMessageToAllReceivers(NotificationPayload.NotificationType type, Serializable... args) {
        Log.i(TAG, "Sending notification of type " + type.toString());
        final List<UserDevice> allUserDevicesWithPermission = requireComponent(PermissionController.KEY)
                .getAllUserDevicesWithPermission(type.getReceivePermission(), null);
        NotificationPayload payload = new NotificationPayload(type, args);
        Message messageToSend = new Message(payload);
        boolean userAtHome;
        /*This might give an error as we do not
          know if the enums and DTOs have the same names*/
        if (type.equals(NotificationPayload.NotificationType.WEATHER_WARNING)) {
            /*If no one is at home everyone should get the WeatherWarning.
              If someone with permission is at home, only them should get a notification.*/
            userAtHome = isSomeoneAtHome(allUserDevicesWithPermission);
            if (userAtHome) {
                sendToUsersAtHome(allUserDevicesWithPermission, messageToSend);
            } else {
                sendToUsers(allUserDevicesWithPermission, messageToSend);
            }
        } else if (type.equals(NotificationPayload.NotificationType.HUMIDITY_WARNING)) {
            userAtHome = isSomeoneAtHome(allUserDevicesWithPermission);
            if (userAtHome) {
                sendToUsersAtHome(allUserDevicesWithPermission, messageToSend);
            } else {
                sendToUsers(allUserDevicesWithPermission, messageToSend);
            }
        } else if (type.equals(NotificationPayload.NotificationType.BRIGHTNESS_WARNING)) {
            userAtHome = isSomeoneAtHome(allUserDevicesWithPermission);
            if (userAtHome) {
                sendToUsersAtHome(allUserDevicesWithPermission, messageToSend);
            } else {
                sendToUsers(allUserDevicesWithPermission, messageToSend);
            }
        } else {
            sendToUsers(allUserDevicesWithPermission, messageToSend);
        }
    }

    /**
     * Searches for userDevices that are currently at home and have the permission to receive the
     * notification. If the userDevice is connected locally it sends him the notification.
     *
     * @param allUserDevicesWithPermission to receive the notification
     * @param messageToSend                content of the notification
     */
    private void sendToUsersAtHome(List<UserDevice> allUserDevicesWithPermission, Message messageToSend) {
        for (UserDevice userDevice : allUserDevicesWithPermission) {
            if (requireComponent(MasterUserLocationHandler.KEY).isDeviceLocal(userDevice.getUserDeviceID())) {
                requireComponent(OutgoingRouter.KEY).sendMessage(userDevice.getUserDeviceID(),
                        RoutingKeys.APP_NOTIFICATION_RECEIVE, messageToSend);
            }
        }
    }

    /**
     * Checks if at least one user, that has the permission to receive the notification, is at home.
     *
     * @param allUserDevicesWithPermission to receive the notification
     * @return true if someone is home, false if no one is home
     */
    private boolean isSomeoneAtHome(List<UserDevice> allUserDevicesWithPermission) {
        for (UserDevice userDevice : allUserDevicesWithPermission) {
            if (requireComponent(MasterUserLocationHandler.KEY).isDeviceLocal(userDevice.getUserDeviceID())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sends out an notification to all userDevices with Permission to receive the notification.
     *
     * @param allUserDevicesWithPermission to receive the notification
     * @param messageToSend                content of the notification
     */
    private void sendToUsers(List<UserDevice> allUserDevicesWithPermission, Message messageToSend) {
        for (UserDevice userDevice : allUserDevicesWithPermission) {
            requireComponent(OutgoingRouter.KEY).sendMessage(userDevice.getUserDeviceID(),
                    RoutingKeys.APP_NOTIFICATION_RECEIVE, messageToSend);
        }
    }
}
