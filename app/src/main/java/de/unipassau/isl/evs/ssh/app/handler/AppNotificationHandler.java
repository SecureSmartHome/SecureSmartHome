package de.unipassau.isl.evs.ssh.app.handler;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.activity.MainActivity;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SystemHealthPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.WeatherPayload;

/**
 * Notification Handler for the App that receives Messages from the MasterNotificationHandler
 * and issues UI Notifications.
 *
 * @author bucher, Chris
 */
public class AppNotificationHandler extends AbstractComponent implements MessageHandler {
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
        //Climate Warnings
        if (message.getPayload() instanceof ClimatePayload) {
            ClimatePayload payload = (ClimatePayload) message.getPayload();
            Context context = getContainer().get(ContainerService.KEY_CONTEXT);
            switch (payload.getNotificationType()) {
                case CoreConstants.NotificationTypes.BRIGHTNESS_WARNING:
                    issueBrightnessWarning(BRIGHTNESS_WARNING_ID, context);
                case CoreConstants.NotificationTypes.HUMIDITY_WARNING:
                    issueClimateNotification(HUMIDITY_WARNING_ID, context);
                    break;
            }
        } else if (message.getPayload() instanceof WeatherPayload) {
            //Weather Warnings
            WeatherPayload payload = (WeatherPayload) message.getPayload();
            issueWeatherWarning(WEATHER_WARNING_ID, payload.getWarnText(), getContainer().get(ContainerService.KEY_CONTEXT));
        } else if (message.getPayload() instanceof SystemHealthPayload) {
            //System Health Warning
            SystemHealthPayload payload = (SystemHealthPayload) message.getPayload();
            issueSystemHealthWarning(SYSTEM_HEALTH_WARNING_ID, payload, getContainer().get(ContainerService.KEY_CONTEXT));
        } else if (message.getPayload() instanceof DoorBellPayload) {
            // Door Bell Notification
            DoorBellPayload payload = ((DoorBellPayload) message.getPayload());
            issueDoorBellNotification(DOOR_BELL_NOTIFICATION_ID, payload, getContainer().get(ContainerService.KEY_CONTEXT));
        }
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {

    }

    @Override
    public void init(Container container) {
        super.init(container);
        container.require(IncomingDispatcher.KEY).registerHandler(this, CoreConstants.RoutingKeys.APP_NOTIFICATION_RECEIVE);
    }

    @Override
    public void destroy() {
        super.destroy();
        getComponent(IncomingDispatcher.KEY).unregisterHandler(this, CoreConstants.RoutingKeys.APP_NOTIFICATION_RECEIVE);
    }

    /**
     * If you want to add a Notification, set a String with the title of the Notification and the
     * message you want to display. Then call displayNotification with these parameters.
     *
     * @param notificationID Is a unique ID for the Notification
     * @param context        Context
     */
    private void issueClimateNotification(int notificationID, Context context) {
        String title = "Climate Warning!";
        String text = "Please open Window! Humidity high.";
        displayNotification(title, text, notificationID, context);
    }

    private void issueBrightnessWarning(int notificationID, Context context) {
        String title = "Light Warning!";
        String text = "Please turn off lights to save energy.";
        displayNotification(title, text, notificationID, context);
    }

    private void issueWeatherWarning(int notificationID, String warnText, Context context) {
        String title = "Weather Warning!";
        displayNotification(title, warnText, notificationID, context);
    }

    private void issueSystemHealthWarning(int notificationID, SystemHealthPayload payload, Context context) {
        String title = "Component failed!";
        Module module = payload.getModule();
        String text = (module.getName() + " at " + module.getAtSlave() + " "
                + module.getModuleType() + " failed.");
        displayNotification(title, text, notificationID, context);
    }

    private void issueDoorBellNotification(int notificationID, DoorBellPayload payload, Context context) {
        String title = "The Door Bell rang";
        String text = ("Door Bell rang at " + payload.getModuleName() + "!");
        displayNotification(title, text, notificationID, context);
    }

    /**
     * Builds the Notification with the given text and displays it on the user-device.
     * If user clicks on it, the ssh app will open.
     *
     * @param title          for the Notification
     * @param text           under the title to give further information
     * @param notificationID unique ID for this type of Notification
     * @param context        Context
     */
    private void displayNotification(String title, String text, int notificationID, Context context) {
        //Build notification
        notificationBuilder.setSmallIcon(R.drawable.ic_home_light);
        notificationBuilder.setColor(2718207);
        notificationBuilder.setWhen(System.currentTimeMillis());
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setContentText(text);

        //TODO send to right Fragment not always to MainActivity
        //If Notification is clicked send to this Page
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);

        //Send notification out to Device
        notificationManager.notify(notificationID, notificationBuilder.build());
    }

    public void addNotificationObjects(NotificationCompat.Builder notificationBuilder,
                                       NotificationManager notificationManager) {
        this.notificationBuilder = notificationBuilder;
        this.notificationManager = notificationManager;
    }
}