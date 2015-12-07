package de.unipassau.isl.evs.ssh.master.task;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.HolidaySimulationPayload;
import de.unipassau.isl.evs.ssh.master.handler.AbstractMasterHandler;

/**
 * This handler calculates what actions need to take place in order to execute the holiday simulation.
 * It then tells the scheduler which HolidayTasks need to be scheduled for which time and also
 * issues a schedule entry for itself, so it is executed again after all planned tasks are finished.
 *
 * @author Chris
 */
public class MasterHolidaySimulationPlannerHandler extends AbstractMasterHandler implements Task {

    private boolean runHolidaySimulation = false;

    @Override
    public void run() {
        //TODO implement
        throw new UnsupportedOperationException();
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (message.getRoutingKey().equals(CoreConstants.RoutingKeys.MASTER_HOLIDAY_GET)) {
            replyStatus(message);
        } else if (message.getRoutingKey().equals(CoreConstants.RoutingKeys.MASTER_HOLIDAY_SET)) {
            if (message.getPayload() instanceof HolidaySimulationPayload) {
                HolidaySimulationPayload payload = (HolidaySimulationPayload) message.getPayload();
                runHolidaySimulation = payload.switchOn();
                replyStatus(message);
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
}