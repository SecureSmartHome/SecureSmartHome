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

package de.unipassau.isl.evs.ssh.master.handler;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.master.MasterConstants;
import de.unipassau.isl.evs.ssh.master.network.broadcast.NotificationBroadcaster;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_PUSH_WEATHER_INFO;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_REQUEST_WEATHER_INFO;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_REQUEST_WEATHER_INFO_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_GET_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_SET_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.BRIGHTNESS_WARNING;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.HUMIDITY_WARNING;

/**
 * Handles climate messages and generates messages for each target and passes them to the OutgoingRouter.
 *
 * @author Christoph Fraedrich
 */
public class MasterClimateHandler extends AbstractMasterHandler implements Component {
    public static final Key<MasterClimateHandler> KEY = new Key<>(MasterClimateHandler.class);

    private static final long WARNING_TIMER = TimeUnit.MINUTES.toMillis(5);

    private long humidityTimeStamp = 0;
    private long brightnessTimeStamp = 0;

    private final Map<Module, ClimatePayload> latestWeatherData = new HashMap<>();
    private final Map<Module, Boolean> latestLightStatus = new HashMap<>();

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_PUSH_WEATHER_INFO, SLAVE_LIGHT_GET_REPLY, MASTER_REQUEST_WEATHER_INFO, SLAVE_LIGHT_SET_REPLY};
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_PUSH_WEATHER_INFO.matches(message)) {
            ClimatePayload payload = MASTER_PUSH_WEATHER_INFO.getPayload(message);
            latestWeatherData.put(payload.getModule(), payload);
            evaluateWeatherData(payload);
        } else if (SLAVE_LIGHT_GET_REPLY.matches(message) || SLAVE_LIGHT_SET_REPLY.matches(message)) {
            //Reply to get request, this means this message actually contains an updated lamp value
            LightPayload payload = message.getPayloadChecked(LightPayload.class);
            latestLightStatus.put(payload.getModule(), payload.getOn());
        } else if (MASTER_REQUEST_WEATHER_INFO.matches(message)) {
            for (ClimatePayload payload : latestWeatherData.values()) {
                sendMessage(message.getFromID(), MASTER_REQUEST_WEATHER_INFO_REPLY, new Message(payload));
            }
        } else {
            invalidMessage(message);
        }
    }

    /**
     * Returns the latest weather data for modules
     *
     * @return map containing ClimatePayloads for Modules
     */
    public Map<Module, ClimatePayload> getLatestWeatherData() {
        return new HashMap<>(latestWeatherData);
    }

    private void evaluateWeatherData(ClimatePayload payload) {
        evaluateHumidity(payload);
        evaluateBrightness(payload);
    }

    private void evaluateBrightness(ClimatePayload payload) {
        NotificationBroadcaster notificationBroadcaster = requireComponent(NotificationBroadcaster.KEY);
        if (payload.getVisible() > MasterConstants.ClimateThreshold.VISIBLE_LIGHT) {
            if (System.currentTimeMillis() - brightnessTimeStamp > WARNING_TIMER) {
                for (Module module : latestLightStatus.keySet()) {
                    if (latestLightStatus.get(module)) {
                        Serializable serializableLight = payload.getVisible();
                        notificationBroadcaster.sendMessageToAllReceivers(BRIGHTNESS_WARNING, serializableLight);
                        brightnessTimeStamp = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    private void evaluateHumidity(ClimatePayload payload) {
        NotificationBroadcaster notificationBroadcaster = requireComponent(NotificationBroadcaster.KEY);
        //The following values will not be checked as they are not of interest: Altitude, Pressure, Temp1, Temp2
        if (payload.getHumidity() > MasterConstants.ClimateThreshold.HUMIDITY) {
            if (System.currentTimeMillis() - humidityTimeStamp > WARNING_TIMER) {
                Serializable serializableHumidity = payload.getHumidity();
                notificationBroadcaster.sendMessageToAllReceivers(HUMIDITY_WARNING, serializableHumidity);
                humidityTimeStamp = System.currentTimeMillis();
            }
        }
    }
}