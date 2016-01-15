package de.unipassau.isl.evs.ssh.app.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.HolidaySimulationPayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_HOLIDAY_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_HOLIDAY_GET_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_HOLIDAY_SET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_HOLIDAY_SET_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_HOLIDAY_SET_REPLY;

/**
 * AppHolidaySimulationHandler class handles messages for the holiday simulation
 *
 * @author Christoph Fraedrich
 */
public class AppHolidaySimulationHandler extends AbstractAppHandler implements Component {
    public static final Key<AppHolidaySimulationHandler> KEY = new Key<>(AppHolidaySimulationHandler.class);
    private static final long REFRESH_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(5);

    private final List<HolidaySimulationListener> listeners = new ArrayList<>();
    private boolean isOn;
    private long lastUpdate = System.currentTimeMillis();

    @Override
    public void handle(Message.AddressedMessage message) {
        if (!tryHandleResponse(message)) {
            if (MASTER_HOLIDAY_GET_REPLY.matches(message) || MASTER_HOLIDAY_SET_REPLY.matches(message)) {
                this.isOn = message.getPayloadChecked(HolidaySimulationPayload.class).switchOn();
                fireHolidayModeSet(true);
            } else if (MASTER_HOLIDAY_SET_ERROR.matches(message)) {
                fireHolidayModeSet(false);
            } else {
                invalidMessage(message);
            }
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_HOLIDAY_GET_REPLY, MASTER_HOLIDAY_SET_REPLY, MASTER_HOLIDAY_SET_ERROR};
    }

    /**
     * Indicates whether the holiday simulation is on
     *
     * @return true if the Holiday Simulation is turned on.
     */
    public boolean isOn() {
        final long now = System.currentTimeMillis();
        if (now - lastUpdate >= REFRESH_DELAY_MILLIS) {
            lastUpdate = now;
            if (getContainer() != null && getContainer().require(NamingManager.KEY).isMasterKnown()) {
                sendMessageToMaster(MASTER_HOLIDAY_GET, new Message(new HolidaySimulationPayload(false)));
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
        final HolidaySimulationPayload payload = new HolidaySimulationPayload(on);
        final Message.AddressedMessage message = sendMessageToMaster(MASTER_HOLIDAY_SET, new Message(payload));
        final Future<HolidaySimulationPayload> future = newResponseFuture(message);

        future.addListener(new FutureListener<HolidaySimulationPayload>() {
            @Override
            public void operationComplete(Future<HolidaySimulationPayload> future) throws Exception {
                boolean isSuccess = future.isSuccess();
                if (isSuccess) {
                    isOn = future.get().switchOn();
                }
                fireHolidayModeSet(isSuccess);
            }
        });
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

    private void fireHolidayModeSet(boolean wasSuccessful) {
        for (HolidaySimulationListener listener : listeners) {
            listener.onHolidaySetReply(wasSuccessful);
        }
    }

    public interface HolidaySimulationListener {
        void onHolidaySetReply(boolean wasSuccessful);
    }
}
