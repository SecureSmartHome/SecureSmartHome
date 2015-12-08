package de.unipassau.isl.evs.ssh.master.task;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorStatusPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.WeatherPayload;
import de.unipassau.isl.evs.ssh.core.schedule.ScheduledComponent;
import de.unipassau.isl.evs.ssh.core.schedule.Scheduler;
import de.unipassau.isl.evs.ssh.master.handler.AbstractMasterHandler;
import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.OpenWeatherMap;

import java.io.IOException;

/**
 * Task/Handler that periodically checks the records of weather data provider and issues notifications
 * based on a configured set of rules.
 *
 * @author Chris
 */
public class MasterWeatherCheckHandler extends AbstractMasterHandler implements ScheduledComponent{

    private static final Key<MasterWeatherCheckHandler> KEY = new Key<>(MasterWeatherCheckHandler.class);
    public static final int MILLIS_IN_FIVE_MIN = 300000;
    private boolean windowClosed;

    private void sendWarningNotification() {
        WeatherPayload payload = new WeatherPayload(true, "Door/Window is open and it will rain today"); //TODO refactor to use generic text
        sendMessageLocal(CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND, new Message(payload));
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof DoorStatusPayload) {
            windowClosed = ((DoorStatusPayload) message.getPayload()).isClosed();
        }
    }

    @Override
    public void init(Container container) {
        Scheduler scheduler = container.require(Scheduler.KEY);
        PendingIntent intent = scheduler.getPendingScheduleIntent(MasterWeatherCheckHandler.KEY, null, 0);
        scheduler.setRepeating(AlarmManager.RTC, System.currentTimeMillis(),
                System.currentTimeMillis() + MILLIS_IN_FIVE_MIN, intent);
    }

    @Override
    public void destroy() {
        Scheduler scheduler = getContainer().require(Scheduler.KEY);
        PendingIntent intent = scheduler.getPendingScheduleIntent(MasterWeatherCheckHandler.KEY, null, 0);
        scheduler.cancel(intent);
    }

    @Override
    public void onReceive(Intent intent) {
        OpenWeatherMap owm = new OpenWeatherMap("f5301a474451c6e1394268314b72a358");
        try {
            CurrentWeather cw = owm.currentWeatherByCityName("Passau");
            if (!windowClosed && cw.getRainInstance().hasRain()) {
                sendWarningNotification();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}