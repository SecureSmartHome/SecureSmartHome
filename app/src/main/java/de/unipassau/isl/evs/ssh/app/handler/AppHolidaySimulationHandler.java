package de.unipassau.isl.evs.ssh.app.handler;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.HolidaySimulationPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

import java.util.ArrayList;
import java.util.List;

/**
 * AppHolidaySimulationHandler class handles messages for the holiday simulation
 *
 *
 * @author Chris
 */
public class AppHolidaySimulationHandler extends AbstractComponent implements MessageHandler{
    public static final Key<AppHolidaySimulationHandler> KEY = new Key<>(AppHolidaySimulationHandler.class);
    private static final String TAG = AppHolidaySimulationHandler.class.getSimpleName();

    private final List<HolidaySimulationListener> listeners = new ArrayList<>();

    public static final int UPDATE_INTERVAL = 5000;
    private boolean isOn;
    private long timeStamp = System.currentTimeMillis();


    @Override
    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof HolidaySimulationPayload) {
            this.isOn = ((HolidaySimulationPayload) message.getPayload()).switchOn();
            fireStatusChanged();
        } else {
            sendErrorMessage(message);
        }
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {

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

    /**
     * Respond with an error message to a given AddressedMessage.
     *
     * @param original Original Message.
     */
    protected Message.AddressedMessage sendErrorMessage(Message.AddressedMessage original) {
        //FIXME logging, reasons??
        Message reply = new Message(new MessageErrorPayload(original.getPayload()));
        reply.putHeader(Message.HEADER_REFERENCES_ID, original.getSequenceNr());
        return sendMessage(
                original.getFromID(),
                original.getHeader(Message.HEADER_REPLY_TO_KEY),
                reply
        );
    }

    protected Message.AddressedMessage sendMessage(DeviceID toID, String routingKey, Message msg) {
        return requireComponent(OutgoingRouter.KEY).sendMessage(toID, routingKey, msg);
    }

    public interface HolidaySimulationListener {
        void statusChanged();
    }

    /**
     * Adds parameter handler to listeners.
     */
    public void addListener(HolidaySimulationListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes parameter handler from listeners.
     */
    public void removeListener(HolidaySimulationListener listener) {
        listeners.remove(listener);
    }

    private void fireStatusChanged() {
        for (HolidaySimulationListener listener: listeners) {
            listener.statusChanged();
        }
    }
}
