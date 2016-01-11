package de.unipassau.isl.evs.ssh.master.task;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.OpenWeatherMap;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.core.schedule.ScheduledComponent;
import de.unipassau.isl.evs.ssh.core.schedule.Scheduler;
import de.unipassau.isl.evs.ssh.master.R;
import de.unipassau.isl.evs.ssh.master.handler.AbstractMasterHandler;
import de.unipassau.isl.evs.ssh.master.network.NotificationBroadcaster;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.FILE_SHARED_PREFS;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_STATUS_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_STATUS_GET_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_STATUS_GET_REPLY;

/**
 * Task/Handler that periodically checks the records of weather data provider and issues notifications
 * based on a configured set of rules.
 *
 * @author Christoph Fraedrich
 */
public class MasterWeatherCheckHandler extends AbstractMasterHandler implements ScheduledComponent {
    private static final long CHECK_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(5);
    private static final Key<MasterWeatherCheckHandler> KEY = new Key<>(MasterWeatherCheckHandler.class);
    private static final String TAG = MasterWeatherCheckHandler.class.getSimpleName();
    private static final long FAILURE_UPDATE_TIMER = 45;

    private long timeStamp;
    private boolean windowOpen;

    private void sendWarningNotification() {
        //No hardcoded strings, only in strings.xml
        NotificationBroadcaster notificationBroadcaster = requireComponent(NotificationBroadcaster.KEY);
        notificationBroadcaster.sendMessageToAllReceivers(NotificationPayload.NotificationType.WEATHER_WARNING);
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_DOOR_STATUS_GET_REPLY.matches(message)) {
            windowOpen = MASTER_DOOR_STATUS_GET_REPLY.getPayload(message).isOpen();
        } else if (MASTER_DOOR_STATUS_GET_ERROR.matches(message)) {
            //TODO Leon: handle (Leon, 11.01.16)
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_DOOR_STATUS_GET_REPLY, MASTER_DOOR_STATUS_GET_ERROR};
    }

    @Override
    public void init(Container container) {
        Scheduler scheduler = container.require(Scheduler.KEY);
        PendingIntent intent = scheduler.getPendingScheduleIntent(MasterWeatherCheckHandler.KEY, null,
                PendingIntent.FLAG_CANCEL_CURRENT);
        scheduler.setRepeating(AlarmManager.RTC, System.currentTimeMillis(),
                CHECK_INTERVAL_MILLIS, intent);
    }

    @Override
    public void destroy() {
        Scheduler scheduler = requireComponent(Scheduler.KEY);
        PendingIntent intent = scheduler.getPendingScheduleIntent(MasterWeatherCheckHandler.KEY, null,
                PendingIntent.FLAG_CANCEL_CURRENT);
        scheduler.cancel(intent);
    }

    @Override
    public void onReceive(Intent intent) {
        OpenWeatherMap owm = new OpenWeatherMap(CoreConstants.OPENWEATHERMAP_API_KEY);
        SharedPreferences sharedPreferences = requireComponent(ContainerService.KEY_CONTEXT).getSharedPreferences(FILE_SHARED_PREFS, Context.MODE_PRIVATE);
        String city = sharedPreferences.getString(String.valueOf(R.string.master_city_name), null);
        try {
            if (city != null) {
                CurrentWeather cw = owm.currentWeatherByCityName(city);
                if (windowOpen && cw.getRainInstance().hasRain()) {
                    sendWarningNotification();
                }
            }
        } catch (IOException e) {
            if (timeStamp - System.currentTimeMillis() > TimeUnit.MINUTES.toMillis(FAILURE_UPDATE_TIMER)) {
                requireComponent(NotificationBroadcaster.KEY).sendMessageToAllReceivers(
                        NotificationPayload.NotificationType.WEATHER_SERVICE_FAILED, city);
                timeStamp = System.currentTimeMillis();
            }
        } catch (Exception e) {
            Log.wtf(TAG, e);
        }
    }
}