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
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;

/**
 * Notification Handler for the App that receives Messages from the MasterNotificationHandler
 * and issues UI Notifications.
 *
 * @author bucher, Chris
 */
public class AppNotificationHandler extends AbstractComponent implements MessageHandler {
    public static final Key<AppNotificationHandler> KEY = new Key<>(AppNotificationHandler.class);

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    /**
     * To add new notification type, add an if-statement with the new message payload.
     *
     * @param message Message to handle.
     */
    @Override
    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof ClimatePayload) {
            ClimatePayload payload = (ClimatePayload) message.getPayload();
            Context context = getContainer().get(ContainerService.KEY_CONTEXT);
            switch (payload.getNotificationType()) {
                case CoreConstants.NotificationTypes.BRIGHTNESS_WARNING:
                    issueBrightnessWarning(2, context);
                case CoreConstants.NotificationTypes.HUMIDITY_WARNING:
                    issueWeatherNotification(1, context);
                    break;
            }
        }
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {

    }

    /**
     * If you want to add a Notification, set a String with the title of the Notification and the
     * message you want to display. Then call displayNotification with these parameters.
     *
     * @param notificationID Is a unique ID for the Notification
     * @param context        Context
     */
    private void issueWeatherNotification(int notificationID, Context context) {
        String title = "Climate Warning!";
        String text = "Please open Window! Humidity high.";
        displayNotification(title, text, notificationID, context);
    }

    private void issueBrightnessWarning(int notificationID, Context context) {
        String title = "Light Warning!";
        String text = "Please turn off lights to save energy.";
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