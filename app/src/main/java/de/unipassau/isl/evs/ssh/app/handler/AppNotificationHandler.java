/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unipassau.isl.evs.ssh.app.handler;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.io.Serializable;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.activity.AppMainActivity;
import de.unipassau.isl.evs.ssh.app.activity.ClimateFragment;
import de.unipassau.isl.evs.ssh.app.activity.DoorFragment;
import de.unipassau.isl.evs.ssh.app.activity.HolidayFragment;
import de.unipassau.isl.evs.ssh.app.activity.LightFragment;
import de.unipassau.isl.evs.ssh.app.activity.MainFragment;
import de.unipassau.isl.evs.ssh.app.activity.StatusFragment;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_NOTIFICATION_RECEIVE;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.BELL_RANG;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.BRIGHTNESS_WARNING;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.DOOR_LOCKED;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.DOOR_UNLATCHED;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.DOOR_UNLOCKED;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.HOLIDAY_MODE_SWITCHED_OFF;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.HOLIDAY_MODE_SWITCHED_ON;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.HUMIDITY_WARNING;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.SYSTEM_HEALTH_WARNING;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.WEATHER_SERVICE_FAILED;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.WEATHER_WARNING;

/**
 * Notification Handler for the App that receives Messages from the MasterNotificationHandler
 * and issues UI Notifications.
 *
 * @author Andreas Bucher, Chris
 */
public class AppNotificationHandler extends AbstractMessageHandler implements Component {
    public static final Key<AppNotificationHandler> KEY = new Key<>(AppNotificationHandler.class);

    private static final String TAG = AppNotificationHandler.class.getSimpleName();

    private Resources resources;

    /**
     * Handles different Notification types.
     *
     * @param message Message to handle.
     */
    @Override
    public void handle(Message.AddressedMessage message) {
        if (APP_NOTIFICATION_RECEIVE.matches(message)) {
            NotificationPayload notificationPayload = (APP_NOTIFICATION_RECEIVE.getPayload(message));
            NotificationType type = notificationPayload.getType();
            Serializable[] args = notificationPayload.getArgs();

            switch (type) {
                case WEATHER_WARNING:
                    issueWeatherWarning(WEATHER_WARNING.ordinal());
                    break;
                case BRIGHTNESS_WARNING:
                    issueBrightnessWarning(BRIGHTNESS_WARNING.ordinal(), args);
                    break;
                case HUMIDITY_WARNING:
                    issueClimateNotification(HUMIDITY_WARNING.ordinal(), args);
                    break;
                case SYSTEM_HEALTH_WARNING:
                    issueSystemHealthWarning(SYSTEM_HEALTH_WARNING.ordinal(), args);
                    break;
                case HOLIDAY_MODE_SWITCHED_ON:
                    issueHolidayModeSwitchedOn(HOLIDAY_MODE_SWITCHED_ON.ordinal());
                    break;
                case HOLIDAY_MODE_SWITCHED_OFF:
                    issueHolidayModeSwitchedOff(HOLIDAY_MODE_SWITCHED_OFF.ordinal());
                    break;
                case BELL_RANG:
                    issueDoorBellNotification(BELL_RANG.ordinal());
                    break;
                case DOOR_UNLATCHED:
                    issueDoorUnlatched(DOOR_UNLATCHED.ordinal());
                    break;
                case DOOR_LOCKED:
                    issueDoorLocked(DOOR_LOCKED.ordinal());
                    break;
                case DOOR_UNLOCKED:
                    issueDoorUnlocked(DOOR_UNLOCKED.ordinal());
                    break;
                case WEATHER_SERVICE_FAILED:
                    issueWeatherServiceFailed(WEATHER_SERVICE_FAILED.ordinal(), args);
                    break;
                case UNKNOWN:
                default:
                    Log.w(TAG, "Can't handle notification with type " + type);
                    break;
            }
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{APP_NOTIFICATION_RECEIVE};
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, RoutingKey routingKey) {
        super.handlerAdded(dispatcher, routingKey);
        resources = requireComponent(ContainerService.KEY_CONTEXT).getResources();
    }

    @Override
    public void handlerRemoved(RoutingKey routingKey) {
        super.handlerRemoved(routingKey);
    }

    /**
     * Handles the different types of notifications. It builds the textual filling for the notifications.
     *
     * @param notificationID Is a unique ID for the Notification
     * @param args           Data (containing e.g. temperature) that are displayed in the notification text.
     */
    private void issueClimateNotification(int notificationID, Serializable[] args) {
        double humidity = (Double) args[0];
        String title = resources.getString(R.string.climate_notification_title);
        String text = String.format(resources.getString(R.string.climate_notification_text), humidity);
        displayNotification(title, text, ClimateFragment.class, notificationID);
    }

    private void issueBrightnessWarning(int notificationID, Serializable[] args) {
        int visibleLight = (Integer) args[0];
        String title = resources.getString(R.string.brightness_warning_title);
        String text = String.format(resources.getString(R.string.brightness_warning_text), visibleLight);
        displayNotification(title, text, LightFragment.class, notificationID);
    }

    private void issueWeatherWarning(int notificationID) {
        String title = resources.getString(R.string.weather_warning_title);
        String text = resources.getString(R.string.weather_warning_text);
        displayNotification(title, text, ClimateFragment.class, notificationID);
    }

    private void issueSystemHealthWarning(int notificationID, Serializable[] args) {

        String moduleName = (String) args[1];

        String title;
        String text;
        if (((boolean) args[0])) {
            title = resources.getString(R.string.system_health_warning_title_failed);
            text = String.format(resources.getString(R.string.system_health_warning_text_failure), moduleName);
        } else {
            title = resources.getString(R.string.system_health_warning_title_fixed);
            text = String.format(resources.getString(R.string.system_health_warning_text_fixed), moduleName);
        }
        displayNotification(title, text, StatusFragment.class, notificationID);
    }

    private void issueDoorBellNotification(int notificationID) {
        String title = resources.getString(R.string.door_bell_notification_title);
        String text = resources.getString(R.string.door_bell_notification_text);
        displayNotification(title, text, DoorFragment.class, notificationID);
    }

    private void issueHolidayModeSwitchedOn(int notificationID) {
        String title = resources.getString(R.string.holiday_mode_switched_on_title);
        String text = resources.getString(R.string.holiday_mode_switched_on_text);
        displayNotification(title, text, HolidayFragment.class, notificationID);
    }

    private void issueHolidayModeSwitchedOff(int notificationID) {
        String title = resources.getString(R.string.holiday_mode_switched_off_title);
        String text = resources.getString(R.string.holiday_mode_switched_off_text);
        displayNotification(title, text, HolidayFragment.class, notificationID);
    }

    private void issueDoorUnlatched(int notificationID) {
        String title = resources.getString(R.string.door_unlatched_title);
        String text = resources.getString(R.string.door_unlatched_text);
        displayNotification(title, text, DoorFragment.class, notificationID);
    }

    private void issueDoorLocked(int notificationID) {
        String title = resources.getString(R.string.door_locked_title);
        String text = resources.getString(R.string.door_locked_text);
        displayNotification(title, text, DoorFragment.class, notificationID);
    }

    private void issueDoorUnlocked(int notificationID) {
        String title = resources.getString(R.string.door_unlocked_title);
        String text = resources.getString(R.string.door_unlocked_text);
        displayNotification(title, text, DoorFragment.class, notificationID);
    }

    private void issueWeatherServiceFailed(int notificationID, Serializable[] args) {
        String cityName = (String) args[0];

        String title = resources.getString(R.string.weather_service_failed_title);
        String text = String.format(resources.getString(R.string.weather_service_failed_text), cityName);

        displayNotification(title, text, MainFragment.class, notificationID);
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
    private void displayNotification(String title, String text, Class openThisFragment, int notificationID) {

        //If Notification is clicked send to this Page
        Context context = requireComponent(ContainerService.KEY_CONTEXT);

        Intent resultIntent = new Intent(context, AppMainActivity.class);
        resultIntent.putExtra(AppMainActivity.KEY_NOTIFICATION_FRAGMENT, openThisFragment);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(AppMainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);

        notificationBuilder.setContentIntent(resultPendingIntent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_home_light)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setLights(R.color.colorPrimary, 3000, 3000);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        //Delete old BellRang if door gets unlocked
        if(notificationID == DOOR_UNLATCHED.ordinal()){
            notificationManager.cancel(BELL_RANG.ordinal());
        }
        //Delete old HolidayMode switched on notification if HolidayMode is switched off
        if(notificationID == HOLIDAY_MODE_SWITCHED_OFF.ordinal()){
            notificationManager.cancel(HOLIDAY_MODE_SWITCHED_ON.ordinal());
        }
        //^vis versa HolidayMode
        if(notificationID == HOLIDAY_MODE_SWITCHED_ON.ordinal()){
            notificationManager.cancel(HOLIDAY_MODE_SWITCHED_OFF.ordinal());
        }
        //Delete old door locked notification if door is unlocked
        if(notificationID == DOOR_UNLOCKED.ordinal()){
            notificationManager.cancel(DOOR_LOCKED.ordinal());
        }
        //^vis versa door notification
        if(notificationID == DOOR_LOCKED.ordinal()){
            notificationManager.cancel(DOOR_UNLOCKED.ordinal());
        }
        //Send notification out to Device
        if (notificationManager != null) {
            notificationManager.notify(notificationID, notificationBuilder.build());
        } else {
            Log.e(TAG, "ERROR! context.getSystemService(Context.NOTIFICATION_SERVICE) was null (AppNotificationHandler)");
        }
    }
}