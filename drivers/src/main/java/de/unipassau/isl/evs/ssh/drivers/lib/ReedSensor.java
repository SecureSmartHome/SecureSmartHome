package de.unipassau.isl.evs.ssh.drivers.lib;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorStatusPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Class to get the values form a window/door sensor
 *
 * @author Wolfram Gottschlich
 * @author Chris
 * @version 1.0
 */

public class ReedSensor extends AbstractComponent {
    public static final Key<ReedSensor> KEY = new Key<>(ReedSensor.class);

    int address;
    int dummyCount;

    private final String moduleName;
    private Container container;
    private ScheduledFuture future;

    /**
     * Constructor of the class representing door and window sensors
     *
     * @param IoAdress where the sensor is connected to the odroid
     * @param moduleName
     */
    public ReedSensor(int IoAdress, String moduleName) {
        this.moduleName = moduleName;
        address = IoAdress;
        dummyCount = 0;
    }

    /**
     * Checks if the window is open
     *
     * @return true if the window is currently open
     */
    public boolean isOpen() throws EvsIoException {
        boolean ret = true;
        if (dummyCount < 5) {
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
                new ReedPollingRunnable(this), 0, 200, TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void destroy() {
        future.cancel(true);
    }


    private class ReedPollingRunnable implements Runnable {

        ReedSensor sensor;

        public ReedPollingRunnable(ReedSensor sensor) {
            this.sensor = sensor;
        }

        @Override
        public void run() {
            try {
                if (future != null && sensor.isOpen()) {
                    sendReedInfo();
                }
            } catch (EvsIoException e) {
                e.printStackTrace();
            }
        }

        /**
         * Sends info about doorbell being used
         */
        private void sendReedInfo() {
            MessagePayload payload = new DoorStatusPayload(moduleName); //TODO FIXME

            NamingManager namingManager = container.require(NamingManager.KEY);

            Message message;
            message = new Message(payload);
            message.putHeader(Message.HEADER_TIMESTAMP, System.currentTimeMillis());

            OutgoingRouter router = container.require(OutgoingRouter.KEY);
            router.sendMessage(namingManager.getMasterID(), CoreConstants.RoutingKeys.MASTER_DOOR_STATUS_GET, message);
        }
    }

}
