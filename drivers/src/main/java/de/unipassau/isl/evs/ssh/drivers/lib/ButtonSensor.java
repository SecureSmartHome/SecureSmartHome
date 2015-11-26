package de.unipassau.isl.evs.ssh.drivers.lib;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Class to get the state of the push button
 *
 * @author Wolfram Gottschlich
 * @version 0.1
 */

public class ButtonSensor extends AbstractComponent{
    public static final Key<ButtonSensor> KEY = new Key<>(ButtonSensor.class);

    int address;
    int dummyCount;

    private final String moduleName;
    private Container container;
    private ScheduledFuture future;

    /**
     * Constructor of the class representing a push button
     *
     * @param address where the button is connected to the odroid
     */
    public ButtonSensor(int IoAdress, String moduleName) {
        this.moduleName = moduleName;
        address = IoAdress;
        dummyCount = 0;
    }

    /**
     * Checks if the push button is currently pressed
     *
     * @return true if the push button is currently pressed
     */
    public boolean isPressed() throws EvsIoException {
        boolean ret = false;
        if (dummyCount < 1000) {
            ret = false;
            dummyCount++;
        } else {
            ret = true;
            dummyCount = 0;
        }
        return ret;
    }

    @Override
    public void init(Container container) {
        super.init(container);
        this.container = container;

        future = container.require(ExecutionServiceComponent.KEY).scheduleAtFixedRate(
                new DoorPollingRunnable(this), 0, 200, TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void destroy() {
        future.cancel(true);
    }


    private class DoorPollingRunnable implements Runnable {

        ButtonSensor sensor;

        public DoorPollingRunnable(ButtonSensor sensor) {
            this.sensor = sensor;
        }

        @Override
        public void run() {
            try {
                if (future != null && sensor.isPressed()) {
                    sendDoorBellInfo();
                }
            } catch (EvsIoException e) {
                e.printStackTrace();
            }
        }

        /**
         * Sends info about doorbell being used
         */
        private void sendDoorBellInfo() {
            DoorBellPayload payload = new DoorBellPayload(moduleName);

            NamingManager namingManager = container.require(NamingManager.KEY);

            Message message;
            message = new Message(payload);
            message.putHeader(Message.HEADER_TIMESTAMP, System.currentTimeMillis());

            OutgoingRouter router = container.require(OutgoingRouter.KEY);
            router.sendMessage(namingManager.getMasterID(), CoreConstants.RoutingKeys.MASTER_DOOR_RINGS, message);
        }
    }
}
