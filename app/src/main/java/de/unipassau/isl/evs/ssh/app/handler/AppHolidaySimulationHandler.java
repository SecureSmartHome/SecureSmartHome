package de.unipassau.isl.evs.ssh.app.handler;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.HolidaySimulationPayload;

/**
 * AppHolidaySimulationHandler class handles messages for the holiday simulation
 *
 *
 * @author Chris
 */
public class AppHolidaySimulationHandler extends AbstractMessageHandler{

    public static final int UPDATE_INTERVAL = 5000;
    private boolean isOn;
    private long timeStamp = System.currentTimeMillis();


    @Override
    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof HolidaySimulationPayload) {
            this.isOn = ((HolidaySimulationPayload) message.getPayload()).switchOn();
        } else {
            sendErrorMessage(message);
        }
    }

    public boolean isOn() {
        if (System.currentTimeMillis() - timeStamp >= UPDATE_INTERVAL) {
            Message message = new Message();
            OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
            router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_HOLIDAY_GET, message);
        }

        return isOn;
    }

    public void switchHolidaySimulation(boolean on) {
        HolidaySimulationPayload payload = new HolidaySimulationPayload(on);
        Message message = new Message(payload);
        OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_HOLIDAY_SET, message);
    }
}
