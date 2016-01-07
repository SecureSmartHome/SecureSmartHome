package de.unipassau.isl.evs.ssh.app.handler;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.io.Serializable;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.NotificationOpenThisFragment.CLIMATE_FRAGMENT;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NotificationOpenThisFragment.DOOR_FRAGMENT;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NotificationOpenThisFragment.HOLIDAY_FRAGMENT;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NotificationOpenThisFragment.LIGHT_FRAGMENT;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NotificationOpenThisFragment.MAIN_FRAGMENT;
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
    private static final int HOLIDAY_MODE_SWITCHED_ON_ID = 6;
    private static final int HOLIDAY_MODE_SWITCHED_OFF_ID = 7;
    private static final int DOOR_UNLATCHED_ID = 8;
    private static final int DOOR_LOCKED = 9;
    private static final int DOOR_UNLOCKED = 10;
    private static final String TAG = AppNotificationHandler.class.getSimpleName();

    /**
     * Handles different Notification types.
     *
     * @param message Message to handle.
     */
    @Override
    public void handle(Message.AddressedMessage message) {
        if (APP_NOTIFICATION_RECEIVE.getPayload(message) != null) {
            if (APP_NOTIFICATION_RECEIVE.getPayload(message) instanceof NotificationPayload) {
                NotificationPayload notificationPayload = (APP_NOTIFICATION_RECEIVE.getPayload(message));
                NotificationPayload.NotificationType type = notificationPayload.getType();
                Serializable[] args = notificationPayload.getArgs();

                switch (type) {
                    case WEATHER_WARNING:
                        issueWeatherWarning(WEATHER_WARNING_ID, args);
                        break;
                    case BRIGHTNESS_WARNING:
                        issueBrightnessWarning(BRIGHTNESS_WARNING_ID, args);
                        break;
                    case HUMIDITY_WARNING:
                        issueClimateNotification(HUMIDITY_WARNING_ID, args);
                        break;
                    case SYSTEM_HEALTH_WARNING:
                        issueSystemHealthWarning(SYSTEM_HEALTH_WARNING_ID, args);
                        break;
                    case HOLIDAY_MODE_SWITCHED_ON:
                        issueHolidayModeSwitchedOn(HOLIDAY_MODE_SWITCHED_ON_ID);
                        break;
                    case HOLIDAY_MODE_SWITCHED_OFF:
                        issueHolidayModeSwitchedOff(HOLIDAY_MODE_SWITCHED_OFF_ID);
                        break;
                    case BELL_RANG:
                        issueDoorBellNotification(DOOR_BELL_NOTIFICATION_ID);
                        break;
                    case DOOR_UNLATCHED:
                        issueDoorUnlatched(DOOR_UNLATCHED_ID);
                        break;
                    case DOOR_LOCKED:
                        issueDoorLocked(DOOR_LOCKED);
                        break;
                    case DOOR_UNLOCKED:
                        issueDoorUnlocked(DOOR_UNLOCKED);
                        break;
                    default:
                        //HANDLE
                        break;
                }
            } else {
                //HANDLE
                Log.e(TAG, "ERROR! Received wrong Payload Type!");
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
     * Handles the different types of notifications. It builds the textual filling for the notifications.
     *
     * @param notificationID Is a unique ID for the Notification
     * @param args           Data (containing e.g. temperature) that are displayed in the notification text.
     */
    //TODO edit text and text formatting
    private void issueClimateNotification(int notificationID, Serializable[] args) {
        double humidity = (Double) args[0];
        String title = Resources.getSystem().getString(R.string.climate_notification_title);
        String text = String.format(Resources.getSystem().getString(R.string.climate_notification_text), humidity);
        displayNotification(title, text, CLIMATE_FRAGMENT, notificationID);
    }

    private void issueBrightnessWarning(int notificationID, Serializable[] args) {
        double visibleLight = (Double) args[0];
        String title = Resources.getSystem().getString(R.string.brightness_warning_title);
        String text = String.format(Resources.getSystem().getString(R.string.brightness_warning_text), visibleLight);
        displayNotification(title, text, LIGHT_FRAGMENT, notificationID);
    }

    private void issueWeatherWarning(int notificationID, Serializable[] args) {
        String title = Resources.getSystem().getString(R.string.weather_warning_title);
        String text = (String) args[0]; //TODO string hardcoded in MasterWeatherCheckHandler
        displayNotification(title, text, CLIMATE_FRAGMENT, notificationID);
    }

    private void issueSystemHealthWarning(int notificationID, Serializable[] args) {
        String title = Resources.getSystem().getString(R.string.system_health_warning_title);
        String moduleName = (String) args[0];
        String text = String.format(Resources.getSystem().getString(R.string.system_health_warning_text), moduleName);
        displayNotification(title, text, STATUS_FRAGMENT, notificationID);
    }

    //TODO add picture to notification?
    private void issueDoorBellNotification(int notificationID) {
        String title = Resources.getSystem().getString(R.string.door_bell_notification_title);
        String text = Resources.getSystem().getString(R.string.door_bell_notification_text);
        displayNotification(title, text, DOOR_FRAGMENT, notificationID);
    }

    private void issueHolidayModeSwitchedOn(int notificationID) {
        String title = Resources.getSystem().getString(R.string.holiday_mode_switched_on_title);
        String text = Resources.getSystem().getString(R.string.holiday_mode_switched_on_text);
        displayNotification(title, text, HOLIDAY_FRAGMENT, notificationID);
    }

    private void issueHolidayModeSwitchedOff(int notificationID) {
        String title = Resources.getSystem().getString(R.string.holiday_mode_switched_off_title);
        String text = Resources.getSystem().getString(R.string.holiday_mode_switched_off_text);
        displayNotification(title, text, HOLIDAY_FRAGMENT, notificationID);
    }

    private void issueDoorUnlatched(int notificationID) {
        String title = Resources.getSystem().getString(R.string.door_unlatched_title);
        String text = Resources.getSystem().getString(R.string.door_unlatched_text);
        displayNotification(title, text, MAIN_FRAGMENT, notificationID);
    }

    private void issueDoorLocked(int notificationID) {
        String title = Resources.getSystem().getString(R.string.door_locked_title);
        String text = Resources.getSystem().getString(R.string.door_locked_text);
        displayNotification(title, text, MAIN_FRAGMENT, notificationID);
    }

    private void issueDoorUnlocked(int notificationID) {
        String title = Resources.getSystem().getString(R.string.door_unlocked_title);
        String text = Resources.getSystem().getString(R.string.door_unlocked_text);
        displayNotification(title, text, MAIN_FRAGMENT, notificationID);
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

        //If Notification is clicked send to this Page
        Context context = getContainer().get(ContainerService.KEY_CONTEXT);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);

       /* Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(openThisFragment);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);
       */
        Intent resultIntent = new Intent(openThisFragment);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(openThisFragment.getClass());
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        notificationBuilder.setContentIntent(resultPendingIntent);

        //Build notification
        notificationBuilder.setSmallIcon(R.drawable.ic_home_light);
        notificationBuilder.setColor(R.color.colorPrimary);
        notificationBuilder.setWhen(System.currentTimeMillis());
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setContentText(text);//maybe obsolete because of bigText style.
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
        notificationBuilder.setVibrate(new long[]{0, 500, 110, 500, 110, 450, 110, 200, 110,
                170, 40, 450, 110, 200, 110, 170, 40, 500});
        notificationBuilder.setLights(R.color.colorPrimary, 3000, 3000);
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notificationBuilder.setSound(alarmSound);


        //Send notification out to Device
        if (context.getSystemService(Context.NOTIFICATION_SERVICE) != null) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(notificationID, notificationBuilder.build());
        } else {
            Log.e(TAG, "ERROR! context.getSystemService(Context.NOTIFICATION_SERVICE) was null (AppNotificationHandler)");
        }
    }
}