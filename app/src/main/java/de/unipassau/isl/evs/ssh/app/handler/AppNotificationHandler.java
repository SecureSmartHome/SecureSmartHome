package de.unipassau.isl.evs.ssh.app.handler;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import java.io.Serializable;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.activity.MainActivity;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.NotificationOpenThisFragment.CLIMATE_FRAGMENT;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NotificationOpenThisFragment.DOOR_FRAGMENT;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NotificationOpenThisFragment.LIGHT_FRAGMENT;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NotificationOpenThisFragment.STATUS_FRAGMENT;
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
    private static final int ODROID_ADDED_ID = 6;
    private static final int HOLIDAY_MODE_SWITCHED_ON_ID = 7;
    private static final int HOLIDAY_MODE_SWITCHED_OFF_ID = 8;
    private static final int DOOR_UNLATCHED_ID = 9;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    /**
     * To add new notification type, add an if-statement with the new message payload.
     *
     * @param message Message to handle.
     */
    @Override
    public void handle(Message.AddressedMessage message) {
        if (APP_NOTIFICATION_RECEIVE.getPayload(message) != null) {
            if(APP_NOTIFICATION_RECEIVE.getPayload(message) instanceof NotificationPayload){
                NotificationPayload notificationPayload = (APP_NOTIFICATION_RECEIVE.getPayload(message));
                NotificationPayload.NotificationType type = notificationPayload.getType();
                Serializable[] args = notificationPayload.getArgs();

                switch(type){
                    case WEATHER_WARNING: issueWeatherWarning(WEATHER_WARNING_ID, args);
                        break;
                    case BRIGHTNESS_WARNING: issueBrightnessWarning(BRIGHTNESS_WARNING_ID, args);
                        break;
                    case HUMIDITY_WARNING: issueClimateNotification(HUMIDITY_WARNING_ID, args);
                        break;
                    case SYSTEM_HEALTH_WARNING: issueSystemHealthWarning(SYSTEM_HEALTH_WARNING_ID, args);
                        break;
                    case ODROID_ADDED: issueOdroidAdded(ODROID_ADDED_ID, args);
                        break;
                    case HOLIDAY_MODE_SWITCHED_ON: issueHolidayModeSwitchedOn(HOLIDAY_MODE_SWITCHED_ON_ID, args);
                        break;
                    case HOLIDAY_MODE_SWITCHED_OFF: issueHolidayModeSwitchedOff(HOLIDAY_MODE_SWITCHED_OFF_ID, args);
                        break;
                    case BELL_RANG: issueDoorBellNotification(DOOR_BELL_NOTIFICATION_ID, args);
                        break;
                    case DOOR_UNLATCHED: issueDoorUnlatched(DOOR_UNLATCHED_ID, args);
                        break;
                }
            }else{
                invalidMessage(message);
            }
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
    //TODO edit title and text
    private void issueClimateNotification(int notificationID, Serializable[] args) {
        String title = "Climate Warning!";
        String text = "Please open Window! Humidity high.";
        displayNotification(title, text, CLIMATE_FRAGMENT, notificationID);
    }

    private void issueBrightnessWarning(int notificationID, Serializable[] args) {
        String title = "Light Warning!";
        String text = "Please turn off lights to save energy.";
        displayNotification(title, text, LIGHT_FRAGMENT, notificationID);
    }

    private void issueWeatherWarning(int notificationID, Serializable[] args) {
        String title = "Weather Warning!";
        String text = "Warn Text!";
        displayNotification(title, text, CLIMATE_FRAGMENT, notificationID);
    }

    private void issueSystemHealthWarning(int notificationID, Serializable[] args) {
        String title = "Component failed!";
        String text = ("ERROR AT: ");
        displayNotification(title, text, STATUS_FRAGMENT, notificationID);
    }

    private void issueDoorBellNotification(int notificationID, Serializable[] args) {
        String title = "The Door Bell rang";
        String text = ("Door Bell rang at front door!");
        displayNotification(title, text, DOOR_FRAGMENT, notificationID);
    }

    private void issueOdroidAdded(int notificationID, Serializable[] args){
        String title = null;
        String text = null;
        displayNotification(title, text, STATUS_FRAGMENT, notificationID);
    }

    private void issueHolidayModeSwitchedOn (int notificationID, Serializable[] args){

    }

    private void issueHolidayModeSwitchedOff (int notificationID, Serializable[] args){

    }

    private void issueDoorUnlatched (int notificationID, Serializable[] args){

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
        notificationBuilder.setColor(R.color.colorPrimaryDark);
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