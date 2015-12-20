package de.unipassau.isl.evs.ssh.master.task;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.HolidayAction;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
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

import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_HOLIDAY_GET;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_HOLIDAY_SET;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.SLAVE_LIGHT_SET;

/**
 * This handler calculates what actions need to take place in order to execute the holiday simulation.
 * It then tells the scheduler which HolidayTasks need to be scheduled for which time and also
 * issues a schedule entry for itself, so it is executed again after all planned tasks are finished.
 *
 * @author Christoph Fraedrich
 */
public class MasterHolidaySimulationPlannerHandler extends AbstractMasterHandler implements ScheduledComponent {
    private static final long SCHEDULE_LOOKAHEAD_MILLIS = TimeUnit.HOURS.toMillis(1);
    private static final Key<MasterHolidaySimulationPlannerHandler> KEY = new Key<>(MasterHolidaySimulationPlannerHandler.class);
    private boolean runHolidaySimulation = false;

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_HOLIDAY_GET.matches(message)) {
            replyStatus(message);
        } else if (MASTER_HOLIDAY_SET.matches(message)) {
            HolidaySimulationPayload payload = MASTER_HOLIDAY_SET.getPayload(message);

            //TODO Refactor if we eliminate one permission
            if (payload.switchOn() && hasPermission(message.getFromID(), new Permission(
                    CoreConstants.Permission.BinaryPermission.START_HOLIDAY_SIMULATION.toString(), ""))) {

                runHolidaySimulation = payload.switchOn();
                replyStatus(message);

            } else if (!payload.switchOn() && hasPermission(message.getFromID(), new Permission(
                    CoreConstants.Permission.BinaryPermission.STOP_HOLIDAY_SIMULATION.toString(), ""))) {

                runHolidaySimulation = payload.switchOn();
                replyStatus(message);

            } else {
                sendErrorMessage(message);
            }
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_HOLIDAY_GET, MASTER_HOLIDAY_SET};
    }

    private void replyStatus(Message.AddressedMessage message) {
        HolidaySimulationPayload payload = new HolidaySimulationPayload(runHolidaySimulation);
        Message reply = new Message(payload);
        sendMessage(message.getFromID(), message.getHeader(Message.HEADER_REPLY_TO_KEY), reply);
    }

    @Override
    public void onReceive(Intent intent) {
        if (runHolidaySimulation) {
            List<HolidayAction> logEntriesRange = getContainer().require(HolidayController.KEY).getHolidayActions(new Date(),
                    new Date(System.currentTimeMillis() + SCHEDULE_LOOKAHEAD_MILLIS));

            for (HolidayAction a : logEntriesRange) {
                int minNow = new Date(System.currentTimeMillis()).getMinutes();
                int minPast = new Date(a.getTimeStamp()).getMinutes();

                if (minPast > minNow) {
                    minPast += 60;
                }

                long delay = minPast - minNow;
                getContainer().require(ExecutionServiceComponent.KEY).schedule(new HolidayLightAction(a.getModuleName(),
                        a.getActionName()), delay, TimeUnit.MINUTES);
            }
        }
    }

    @Override
    public void init(Container container) {
        Scheduler scheduler = getContainer().require(Scheduler.KEY);
        PendingIntent intent = scheduler.getPendingScheduleIntent(MasterHolidaySimulationPlannerHandler.KEY, null, 0);
        scheduler.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), SCHEDULE_LOOKAHEAD_MILLIS, intent);
    }

    @Override
    public void destroy() {
        //FIXME cancel scheduled task (Niko, 2015-12-17)
    }

    private class HolidayLightAction implements Runnable {

        String moduleName;
        String actionName;

        public HolidayLightAction(String moduleName, String actionName) {
            this.moduleName = moduleName;
            this.actionName = actionName;
        }

        @Override
        public void run() {
            boolean on = false;
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