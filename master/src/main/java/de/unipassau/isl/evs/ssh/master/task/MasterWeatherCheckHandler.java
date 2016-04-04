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

package de.unipassau.isl.evs.ssh.master.task;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.common.base.Strings;

import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.OpenWeatherMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorStatusPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
import de.unipassau.isl.evs.ssh.core.schedule.ScheduledComponent;
import de.unipassau.isl.evs.ssh.core.schedule.Scheduler;
import de.unipassau.isl.evs.ssh.master.R;
import de.unipassau.isl.evs.ssh.master.handler.AbstractMasterHandler;
import de.unipassau.isl.evs.ssh.master.network.broadcast.NotificationBroadcaster;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_STATUS_UPDATE;

/**
 * Task/Handler that periodically checks the records of weather data provider and issues notifications
 * based on a configured set of rules.
 *
 * @author Christoph Fraedrich
 */
public class MasterWeatherCheckHandler extends AbstractMasterHandler implements ScheduledComponent {
    public static final Key<MasterWeatherCheckHandler> KEY = new Key<>(MasterWeatherCheckHandler.class);
    private static final long CHECK_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(5);
    private static final long FAILURE_UPDATE_TIMER = TimeUnit.MINUTES.toMillis(45);
    private static final String TAG = MasterWeatherCheckHandler.class.getSimpleName();

    private Container container;
    private long timeStamp = -1;
    private final Map<String, Boolean> openForModule = new HashMap<>();
    private final OpenWeatherMap owm = new OpenWeatherMap(CoreConstants.OPENWEATHERMAP_API_KEY);

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_DOOR_STATUS_UPDATE.matches(message)) {
            final DoorStatusPayload doorStatusPayload = MASTER_DOOR_STATUS_UPDATE.getPayload(message);
            openForModule.put(doorStatusPayload.getModuleName(), doorStatusPayload.isOpen());
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_DOOR_STATUS_UPDATE};
    }

    @Override
    public void init(Container container) {
        this.container = container;
        container.require(IncomingDispatcher.KEY).registerHandler(this, getRoutingKeys());
        Scheduler scheduler = container.require(Scheduler.KEY);
        PendingIntent intent = scheduler.getPendingScheduleIntent(MasterWeatherCheckHandler.KEY, null,
                PendingIntent.FLAG_CANCEL_CURRENT);
        scheduler.setRepeating(AlarmManager.RTC, System.currentTimeMillis(),
                CHECK_INTERVAL_MILLIS, intent);
    }

    @Override
    public void destroy() {
        if (container != null) {
            Scheduler scheduler = requireComponent(Scheduler.KEY);
            PendingIntent intent = scheduler.getPendingScheduleIntent(MasterWeatherCheckHandler.KEY, null,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            scheduler.cancel(intent);

            requireComponent(IncomingDispatcher.KEY).unregisterHandler(this, getRoutingKeys());
        }
        this.container = null;
    }

    @Override
    public void onReceive(Intent intent) {
        SharedPreferences sharedPreferences = requireComponent(ContainerService.KEY_CONTEXT).getSharedPreferences();
        final String city = sharedPreferences.getString(
                requireComponent(ContainerService.KEY_CONTEXT).getResources().getString(R.string.master_city_name),
                null
        );
        if (Strings.isNullOrEmpty(city)) {
            return;
        }

        Log.i(TAG, "Inquiring weather data for " + city);
        //Presentation Mode
        if (city.equals("Mordor")) {
            for (Boolean isOpen : openForModule.values()) {
                if (isOpen) {
                    sendWarningNotification();
                    break;
                }
            }
            return;
        }

        requireComponent(ExecutionServiceComponent.KEY).execute(new Runnable() {
            @Override
            public void run() {
                try {
                    CurrentWeather cw = owm.currentWeatherByCityName(city);
                    if (cw == null || cw.getRainInstance() == null) {
                        WeatherServiceFailed(city);
                        return;
                    }

                    if (cw.getRainInstance().hasRain()) {
                        for (Boolean isOpen : openForModule.values()) {
                            if (isOpen) {
                                sendWarningNotification();
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                    WeatherServiceFailed(city);
                } catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
            }
        });
    }

    private void WeatherServiceFailed(String city) {
        if (timeStamp == -1 || timeStamp - System.currentTimeMillis() > FAILURE_UPDATE_TIMER) {
            requireComponent(NotificationBroadcaster.KEY).sendMessageToAllReceivers(
                    NotificationPayload.NotificationType.WEATHER_SERVICE_FAILED, city);
            timeStamp = System.currentTimeMillis();
        }
    }

    private void sendWarningNotification() {
        //No hardcoded strings, only in strings.xml
        NotificationBroadcaster notificationBroadcaster = requireComponent(NotificationBroadcaster.KEY);
        notificationBroadcaster.sendMessageToAllReceivers(NotificationPayload.NotificationType.WEATHER_WARNING);
    }
}