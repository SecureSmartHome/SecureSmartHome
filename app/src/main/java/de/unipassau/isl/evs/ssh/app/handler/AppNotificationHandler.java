package de.unipassau.isl.evs.ssh.app.handler;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;
import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.activity.ClimateFragment;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.WeatherPayload;

/**
 * Notification Handler for the App that receives Messages from the MasterNotificationHandler
 * and issues UI Notifications.
 *
 * @author bucher, Chris
 */
public class AppNotificationHandler extends AbstractComponent implements MessageHandler{
    public static final Key<AppNotificationHandler> KEY = new Key<>(AppNotificationHandler.class);

    private final NotificationManager notificationManager;
    private final NotificationCompat.Builder notificationBuilder;

    public AppNotificationHandler(NotificationManager notficationManager, NotificationCompat.Builder notificationBuilder) {
        this.notificationManager = notficationManager;
        this.notificationBuilder = notificationBuilder;
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof WeatherPayload) {
            WeatherPayload payload = (WeatherPayload) message.getPayload();
            switch (payload.getNotificationType()) {
                case CoreConstants.NotificationTypes.BRIGHTNESS_WARNING:
                    //TODO
                case CoreConstants.NotificationTypes.HUMIDITY_WARNING:
                    issueWeatherNotification(1);
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

    private void issueWeatherNotification(int notificationID) {
        notificationBuilder.setSmallIcon(R.drawable.ic_home_light);
        notificationBuilder.setContentTitle("Climate Warning!");
        notificationBuilder.setWhen(System.currentTimeMillis());
        notificationBuilder.setContentText("Please open Window! Humidity high.");

        //If Notification is clicked send to this Page
        Intent intent = new Intent(getContainer().get(ContainerService.KEY_CONTEXT), ClimateFragment.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getContainer().get(ContainerService.KEY_CONTEXT),
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);

        //Send notification out to Device
        notificationManager.notify(notificationID, notificationBuilder.build());
    }
}
