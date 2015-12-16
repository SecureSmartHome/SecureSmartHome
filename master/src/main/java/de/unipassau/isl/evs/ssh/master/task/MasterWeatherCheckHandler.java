package de.unipassau.isl.evs.ssh.master.task;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.OpenWeatherMap;

import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorStatusPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.WeatherPayload;
import de.unipassau.isl.evs.ssh.core.schedule.ScheduledComponent;
import de.unipassau.isl.evs.ssh.core.schedule.Scheduler;
import de.unipassau.isl.evs.ssh.master.handler.AbstractMasterHandler;

/**
 * Task/Handler that periodically checks the records of weather data provider and issues notifications
 * based on a configured set of rules.
 *
 * @author Christoph Fraedrich
 */
public class MasterWeatherCheckHandler extends AbstractMasterHandler implements ScheduledComponent {
    public static final long MILLIS_IN_FIVE_MIN = TimeUnit.MINUTES.toMillis(5);
    private static final Key<MasterWeatherCheckHandler> KEY = new Key<>(MasterWeatherCheckHandler.class);
    private static final String TAG = MasterWeatherCheckHandler.class.getSimpleName();
    private boolean windowClosed;

    private void sendWarningNotification() {
        WeatherPayload payload = new WeatherPayload(true, "Door/Window is open and it will rain today"); //TODO refactor to use generic text
        sendMessageLocal(CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND, new Message(payload));
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        //FIXME //STOPSHIP use correct RoutingKey instead and add to getRoutingKeys() (Niko, 2015-12-17)
        if (message.getPayload() instanceof DoorStatusPayload) {
            windowClosed = ((DoorStatusPayload) message.getPayload()).isClosed();
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{};
    }

    @Override
    public void init(Container container) {
        Scheduler scheduler = container.require(Scheduler.KEY);
        PendingIntent intent = scheduler.getPendingScheduleIntent(MasterWeatherCheckHandler.KEY, null,
                PendingIntent.FLAG_CANCEL_CURRENT);
        scheduler.setRepeating(AlarmManager.RTC, System.currentTimeMillis(),
                MILLIS_IN_FIVE_MIN, intent);
    }

    @Override
    public void destroy() {
        Scheduler scheduler = getContainer().require(Scheduler.KEY);
        PendingIntent intent = scheduler.getPendingScheduleIntent(MasterWeatherCheckHandler.KEY, null,
                PendingIntent.FLAG_CANCEL_CURRENT);
        scheduler.cancel(intent);
    }

    @Override
    public void onReceive(Intent intent) {
        OpenWeatherMap owm = new OpenWeatherMap("f5301a474451c6e1394268314b72a358"); //TODO move to CoreConstants (Niko, 2015-12-17)
        try {
            CurrentWeather cw = owm.currentWeatherByCityName("Passau"); //TODO use current location  (Niko, 2015-12-17)
            if (!windowClosed && cw.getRainInstance().hasRain()) {
                sendWarningNotification();
            }
        } catch (Exception e) {
            Log.wtf(TAG, e);
        }
    }
}