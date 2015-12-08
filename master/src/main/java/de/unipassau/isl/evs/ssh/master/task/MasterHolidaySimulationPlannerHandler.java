package de.unipassau.isl.evs.ssh.master.task;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.HolidayAction;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.HolidaySimulationPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
import de.unipassau.isl.evs.ssh.master.database.DatabaseContract;
import de.unipassau.isl.evs.ssh.master.database.HolidayController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.handler.AbstractMasterHandler;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This handler calculates what actions need to take place in order to execute the holiday simulation.
 * It then tells the scheduler which HolidayTasks need to be scheduled for which time and also
 * issues a schedule entry for itself, so it is executed again after all planned tasks are finished.
 *
 * @author Chris
 */
public class MasterHolidaySimulationPlannerHandler extends AbstractMasterHandler implements Task {

    public static final int MILLIS_PER_HOUR = 3600000;
    private boolean runHolidaySimulation = false;

    public MasterHolidaySimulationPlannerHandler() {
        getContainer().require(ExecutionServiceComponent.KEY).scheduleAtFixedRate(this, 0, MILLIS_PER_HOUR,
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void run() {
        if (runHolidaySimulation) {
            List<HolidayAction> logEntriesRange = getContainer().require(HolidayController.KEY).getHolidayActions(new Date(),
                    new Date(System.currentTimeMillis() + MILLIS_PER_HOUR));

            for (HolidayAction a: logEntriesRange) {
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
    public void handle(Message.AddressedMessage message) {
        if (message.getRoutingKey().equals(CoreConstants.RoutingKeys.MASTER_HOLIDAY_GET)) {
            replyStatus(message);

        } else if (message.getRoutingKey().equals(CoreConstants.RoutingKeys.MASTER_HOLIDAY_SET)) {
            if (message.getPayload() instanceof HolidaySimulationPayload) {
                HolidaySimulationPayload payload = (HolidaySimulationPayload) message.getPayload();

                //TODO Refactor if we eliminate one permission
                if (payload.switchOn() && hasPermission(message.getFromID(),new Permission(
                                DatabaseContract.Permission.Values.START_HOLIDAY_SIMULATION, ""))) {

                    runHolidaySimulation = payload.switchOn();
                    replyStatus(message);

                } else if (!payload.switchOn() && hasPermission(message.getFromID(),new Permission(
                        DatabaseContract.Permission.Values.STOP_HOLIDAY_SIMULATION, ""))) {

                    runHolidaySimulation = payload.switchOn();
                    replyStatus(message);

                } else {
                    sendErrorMessage(message);
                }
            } else {
                sendErrorMessage(message);
            }
        } else {
            sendErrorMessage(message);
        }
    }

    private void replyStatus(Message.AddressedMessage message) {
        HolidaySimulationPayload payload = new HolidaySimulationPayload(runHolidaySimulation);
        Message reply = new Message(payload);
        sendMessage(message.getFromID(), message.getHeader(Message.HEADER_REPLY_TO_KEY), reply);
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
            boolean on=false;
            Module module = getContainer().require(SlaveController.KEY).getModule(moduleName);
            if (actionName.equals(CoreConstants.LogActions.LIGHT_ON_ACTION)) {
                on = true;
            } else if (actionName.equals(CoreConstants.LogActions.LIGHT_OFF_ACTION)) {
                on = false;
            }
            MessagePayload payload = new LightPayload(on, module);
            Message message = new Message(payload);
            getContainer().require(OutgoingRouter.KEY).sendMessage(module.getAtSlave(),
                    CoreConstants.RoutingKeys.SLAVE_LIGHT_SET, message);
        }
    }
}