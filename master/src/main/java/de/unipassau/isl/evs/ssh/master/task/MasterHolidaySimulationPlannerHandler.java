package de.unipassau.isl.evs.ssh.master.task;

import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * This handler calculates what actions need to take place in order to execute the holiday simulation.
 * It then tells the scheduler which HolidayTasks need to be scheduled for which time and also
 * issues a schedule entry for itself, so it is executed again after all planned tasks are finished.
 */
public class MasterHolidaySimulationPlannerHandler implements Task, Handler {
    @Override
    public void handle(Message message) {
        //TODO implement
        throw new UnsupportedOperationException();
    }

    @Override
    public void run() {
        //TODO implement
        throw new UnsupportedOperationException();
    }
}