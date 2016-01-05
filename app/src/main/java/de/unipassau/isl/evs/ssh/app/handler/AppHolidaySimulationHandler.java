package de.unipassau.isl.evs.ssh.app.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.HolidaySimulationPayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_HOLIDAY_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_HOLIDAY_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_HOLIDAY_SET;

/**
 * AppHolidaySimulationHandler class handles messages for the holiday simulation
 *
 * @author Christoph Fraedrich
 */
public class AppHolidaySimulationHandler extends AbstractMessageHandler implements Component {
    public static final Key<AppHolidaySimulationHandler> KEY = new Key<>(AppHolidaySimulationHandler.class);
    private static final long REFRESH_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(5);

    private final List<HolidaySimulationListener> listeners = new ArrayList<>();
    private boolean isOn;
    private long lastUpdate = System.currentTimeMillis();

    @Override
    public void handle(Message.AddressedMessage message) {
        if (APP_HOLIDAY_GET.matches(message)) {
            this.isOn = APP_HOLIDAY_GET.getPayload(message).switchOn();
            fireStatusChanged();
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_HOLIDAY_SET};
    }

    /**
     * @return if the Holiday Simulation is turned on.
     */
    public boolean isOn() {
        if (System.currentTimeMillis() - lastUpdate >= REFRESH_DELAY_MILLIS) {
            if (getContainer().require(NamingManager.KEY).isMasterKnown()) {
                Message message = new Message(new HolidaySimulationPayload(false));
                message.putHeader(Message.HEADER_REPLY_TO_KEY, APP_HOLIDAY_GET.getKey());
                sendMessageToMaster(MASTER_HOLIDAY_GET, new Message(
                        new HolidaySimulationPayload(false)));
            }
        }
        return isOn;
    }

    /**
     * Changes the statue of the Holiday Simulation. When parameter on is {@code true} the simulation will start.
     * Otherwise the Simulation will stop.
     *
     * @param on {@code true} to start the holiday simulation, {@code false} to stop the holiday simulation.
     */
    public void switchHolidaySimulation(boolean on) {
        HolidaySimulationPayload payload = new HolidaySimulationPayload(on);
        Message message = new Message(payload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, APP_HOLIDAY_GET.getKey());
        sendMessageToMaster(MASTER_HOLIDAY_SET, message);
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
        for (HolidaySimulationListener listener : listeners) {
            listener.statusChanged();
        }
    }

    public interface HolidaySimulationListener {
        void statusChanged();
    }
}
