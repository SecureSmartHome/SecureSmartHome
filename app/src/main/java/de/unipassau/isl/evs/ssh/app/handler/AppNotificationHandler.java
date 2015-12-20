package de.unipassau.isl.evs.ssh.app.handler;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.activity.MainActivity;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SystemHealthPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.WeatherPayload;
import de.unipassau.isl.evs.ssh.core.sec.Permission;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_NOTIFICATION_RECEIVE;

/**
 * Notification Handler for the App that receives Messages from the MasterNotificationHandler
 * and issues UI Notifications.
 *
 * @author Andreas Bucher, Chris
 */
public class AppNotificationHandler extends AbstractMessageHandler implements Component {
    public static final Key<AppNotificationHandler> KEY = new Key<>(AppNotificationHandler.class);

    private static final int HUMIDITY_WARNING_ID = 1;
    private static final int BRIGHTNESS_WARNING_ID = 2;
    private static final int WEATHER_WARNING_ID = 3;
    private static final int SYSTEM_HEALTH_WARNING_ID = 4;
    private static final int DOOR_BELL_NOTIFICATION_ID = 5;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    /**
     * To add new notification type, add an if-statement with the new message payload.
     *
     * @param message Message to handle.
     */
    @Override
    public void handle(Message.AddressedMessage message) {
        //FIXME //STOPSHIP will not work with new RoutingKeys (Niko, 2015-12-16)
        //TODO either notificationpayload or switch case for other payloads
        if (message.getPayload() instanceof NotificationPayload) {
            NotificationPayload notificationPayload = ((NotificationPayload) message.getPayload());
            //TODO make openthisfragment constants in coreconstants
            displayNotification("Notification", notificationPayload.getMessage(), "ClimateFragment", 55);
            //Climate Warnings
        } else if (message.getPayload() instanceof ClimatePayload) {
            ClimatePayload payload = (ClimatePayload) message.getPayload();

            //TODO comparing the notification type name with the name of a permission?? that must be a tough mental typecast (Niko, 2015-12-20)
            if (payload.getNotificationType().equals(Permission.BRIGHTNESS_WARNING.toString())) {
                issueBrightnessWarning(BRIGHTNESS_WARNING_ID);
                issueClimateNotification(HUMIDITY_WARNING_ID);
            } else if (payload.getNotificationType().equals(Permission.HUMIDITY_WARNING.toString())) {
                issueClimateNotification(HUMIDITY_WARNING_ID);
            }
            //Weather Warnings
        } else if (message.getPayload() instanceof WeatherPayload) {
            WeatherPayload payload = (WeatherPayload) message.getPayload();
            issueWeatherWarning(WEATHER_WARNING_ID, payload.getWarnText());
            //System Health Warning
        } else if (message.getPayload() instanceof SystemHealthPayload) {
            SystemHealthPayload payload = (SystemHealthPayload) message.getPayload();
            issueSystemHealthWarning(SYSTEM_HEALTH_WARNING_ID, payload);
        } else if (message.getPayload() instanceof DoorBellPayload) {
            // Door Bell Notification
            DoorBellPayload payload = ((DoorBellPayload) message.getPayload());
            issueDoorBellNotification(DOOR_BELL_NOTIFICATION_ID, payload);
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{APP_NOTIFICATION_RECEIVE};
    }

    /**
     * If you want to add a Notification, set a String with the title of the Notification and the
     * message you want to display. Then call displayNotification with these parameters.
     *
     * @param notificationID Is a unique ID for the Notification
     */
    private void issueClimateNotification(int notificationID) {
        String title = "Climate Warning!";
        String text = "Please open Window! Humidity high.";
        displayNotification(title, text, "ClimateFragment", notificationID);
    }

    private void issueBrightnessWarning(int notificationID) {
        String title = "Light Warning!";
        String text = "Please turn off lights to save energy.";
        displayNotification(title, text, "LightFragment", notificationID);
    }

    private void issueWeatherWarning(int notificationID, String warnText) {
        String title = "Weather Warning!";
        displayNotification(title, warnText, "ClimateFragment", notificationID);
    }

    private void issueSystemHealthWarning(int notificationID, SystemHealthPayload payload) {
        String title = "Component failed!";
        Module module = payload.getModule();
        String text = (module.getName() + " at " + module.getAtSlave() + " "
                + module.getModuleType() + " failed.");
        displayNotification(title, text, "StatusFragment", notificationID);
    }

    private void issueDoorBellNotification(int notificationID, DoorBellPayload payload) {
        String title = "The Door Bell rang";
        String text = ("Door Bell rang at " + payload.getModuleName() + "!");
        displayNotification(title, text, "DoorFragment", notificationID);
    }

    /**
     * Builds the Notification with the given text and displays it on the user-device.
     * If user clicks on it, the ssh app will open.
     *
     * @param title            for the Notification
     * @param text             under the title to give further information
     * @param openThisFragment Which fragment should be opened when clicked on the notification
     *                         Add string to MainActivity (onCreate) if not already declared.
     * @param notificationID   unique ID for this type of Notification
     */
    private void displayNotification(String title, String text, String openThisFragment, int notificationID) {
        final int REQUEST_CODE = 0;

        //Build notification
        notificationBuilder.setSmallIcon(R.drawable.ic_home_light);
        notificationBuilder.setColor(2718207);//TODO use resource instead (Niko, 2015-12-16)
        notificationBuilder.setWhen(System.currentTimeMillis());
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setContentText(text);

        //If Notification is clicked send to this Page
        Context context = getContainer().get(ContainerService.KEY_CONTEXT);
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(openThisFragment);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);

        //Send notification out to Device
        notificationManager.notify(notificationID, notificationBuilder.build());
    }

    //FIXME why is this passed in by the MainActivity? (Niko, 2015-12-16)
    public void addNotificationObjects(NotificationCompat.Builder notificationBuilder,
                                       NotificationManager notificationManager) {
        this.notificationBuilder = notificationBuilder;
        this.notificationManager = notificationManager;
    }
}