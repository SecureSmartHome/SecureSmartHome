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
import android.os.SystemClock;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.HolidayAction;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.HolidaySimulationPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
import de.unipassau.isl.evs.ssh.core.schedule.ScheduledComponent;
import de.unipassau.isl.evs.ssh.core.schedule.Scheduler;
import de.unipassau.isl.evs.ssh.master.database.HolidayController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.handler.AbstractMasterHandler;
import de.unipassau.isl.evs.ssh.master.network.broadcast.NotificationBroadcaster;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_HOLIDAY_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_HOLIDAY_SET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_SET;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.HOLIDAY_MODE_SWITCHED_OFF;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.HOLIDAY_MODE_SWITCHED_ON;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.TOGGLE_HOLIDAY_SIMULATION;

/**
 * This handler calculates what actions need to take place in order to execute the holiday simulation.
 * It then tells the scheduler which HolidayTasks need to be scheduled for which time and also
 * issues a schedule entry for itself, so it is executed again after all planned tasks are finished.
 *
 * @author Christoph Fraedrich
 */
public class MasterHolidaySimulationPlannerHandler extends AbstractMasterHandler implements ScheduledComponent {
    private static final long SCHEDULE_LOOKAHEAD_MILLIS = TimeUnit.HOURS.toMillis(1);
    public static final Key<MasterHolidaySimulationPlannerHandler> KEY = new Key<>(MasterHolidaySimulationPlannerHandler.class);
    private static final String TAG = MasterHolidaySimulationPlannerHandler.class.getSimpleName();

    private boolean runHolidaySimulation = false;

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_HOLIDAY_GET, MASTER_HOLIDAY_SET};
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_HOLIDAY_GET.matches(message)) {
            replyStatus(message);
        } else if (MASTER_HOLIDAY_SET.matches(message)) {
            MASTER_HOLIDAY_SET.getPayload(message);
            handleHolidaySet(message, MASTER_HOLIDAY_SET.getPayload(message));
        } else {
            invalidMessage(message);
        }
    }

    private void handleHolidaySet(Message.AddressedMessage message, HolidaySimulationPayload holidaySimulationPayload) {
        if (hasPermission(message.getFromID(), TOGGLE_HOLIDAY_SIMULATION)) {
            runHolidaySimulation = holidaySimulationPayload.switchOn();
            replyStatus(message);
            final NotificationBroadcaster notificationBroadcaster = requireComponent(NotificationBroadcaster.KEY);
            if (runHolidaySimulation) {
                notificationBroadcaster.sendMessageToAllReceivers(HOLIDAY_MODE_SWITCHED_ON);
                Scheduler scheduler = requireComponent(Scheduler.KEY);
                PendingIntent intent = scheduler.getPendingScheduleIntent(MasterHolidaySimulationPlannerHandler.KEY, null, PendingIntent.FLAG_CANCEL_CURRENT);
                scheduler.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + 1000, SCHEDULE_LOOKAHEAD_MILLIS, intent);
            } else {
                Scheduler scheduler = requireComponent(Scheduler.KEY);
                PendingIntent intent = scheduler.getPendingScheduleIntent(MasterHolidaySimulationPlannerHandler.KEY, null, PendingIntent.FLAG_CANCEL_CURRENT);
                scheduler.cancel(intent);
                notificationBroadcaster.sendMessageToAllReceivers(HOLIDAY_MODE_SWITCHED_OFF);
                //TODO Leon: kill all planned tasks (Leon, 14.01.16)
            }
        } else {
            sendNoPermissionReply(message, TOGGLE_HOLIDAY_SIMULATION);
        }
    }

    private void replyStatus(Message.AddressedMessage message) {
        HolidaySimulationPayload payload = new HolidaySimulationPayload(runHolidaySimulation);
        Message reply = new Message(payload);
        sendReply(message, reply);
    }

    @Override
    public void onReceive(Intent intent) {
        Log.i(TAG, "HolidayPlanner calculating...");
        //Cannot do anything without the container
        if (runHolidaySimulation && getContainer() != null) {
            final long planningStartTime = System.currentTimeMillis();
            //replays last week
            List<HolidayAction> lastWeek = requireComponent(HolidayController.KEY).getHolidayActions(
                    planningStartTime - TimeUnit.DAYS.toMillis(7),
                    planningStartTime - TimeUnit.DAYS.toMillis(7) + SCHEDULE_LOOKAHEAD_MILLIS
            );

            for (HolidayAction a : lastWeek) {
                requireComponent(ExecutionServiceComponent.KEY).schedule(new HolidayLightAction(a.getModuleName(),
                        a.getActionName()), (a.getTimeStamp() + TimeUnit.DAYS.toMillis(7) - planningStartTime) / 1000,
                        TimeUnit.SECONDS);
            }
        }
    }

    @Override
    public void init(Container container) {
        super.init(container);
    }

    @Override
    public void destroy() {
        if (getContainer() != null) {
            //TODO Leon: only cancel, when not already canceled (Leon, 14.01.16)
            Scheduler scheduler = requireComponent(Scheduler.KEY);
            PendingIntent intent = scheduler.getPendingScheduleIntent(MasterHolidaySimulationPlannerHandler.KEY, null, PendingIntent.FLAG_CANCEL_CURRENT);
            scheduler.cancel(intent);
        }
        super.destroy();
    }

    /**
     * Private class representing an action which has to be executed when the holiday simulation
     * is active.
     */
    private class HolidayLightAction implements Runnable {

        final String moduleName;
        final String actionName;

        public HolidayLightAction(String moduleName, String actionName) {
            this.moduleName = moduleName;
            this.actionName = actionName;
        }

        @Override
        public void run() {
            boolean on = false;
            if (getContainer() != null) {
                Module module = getContainer().require(SlaveController.KEY).getModule(moduleName);
                if (actionName.equals(CoreConstants.LogActions.LIGHT_ON_ACTION)) {
                    on = true;
                } else if (actionName.equals(CoreConstants.LogActions.LIGHT_OFF_ACTION)) {
                    on = false;
                }
                MessagePayload payload = new LightPayload(on, module);
                Message message = new Message(payload);
                sendMessage(module.getAtSlave(), SLAVE_LIGHT_SET, message);
            }
        }
    }

    public boolean isRunHolidaySimulation() {
        return runHolidaySimulation;
    }
}