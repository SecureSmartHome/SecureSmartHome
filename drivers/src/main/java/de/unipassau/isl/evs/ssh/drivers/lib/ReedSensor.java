package de.unipassau.isl.evs.ssh.drivers.lib;

import android.util.Log;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorStatusPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;

/**
 * Class to get the values form a window/door sensor
 *
 * @author Christoph Frädrich
 * @author Wolfram Gottschlich
 * @version 2.0
 */

public class ReedSensor extends AbstractComponent {
    public static final Key<ReedSensor> KEY = new Key<>(ReedSensor.class);
    private final String moduleName;
    int address;
    private Container container;
    private ScheduledFuture future;

    /**
     * Constructor of the class representing door and window sensors
     *
     * @param IoAdress where the sensor is connected to the odroid
     */
    public ReedSensor(int IoAdress, String moduleName) throws EvsIoException {
        this.moduleName = moduleName;
        EvsIo.registerPin(IoAdress, "in");
    }

    /**
     * Checks if the window is open
     *
     * @return true if the window is currently open
     */
    public boolean isOpen() throws EvsIoException {
        boolean ret = true;
        String result = "";
        result = EvsIo.readValue(address);
        //Log.w("EVS-IO", "EVS-REED:" + result + ":");


        if (result.startsWith("1")) {
            ret = true;
        } else {
            ret = false;
        }

        return ret;

        // TODO simplify this method (probably not our job) (Wolfgang, 2015-12-31)
        // Here is an alternative implementation of this method:
        //return EvsIo.readValue(address).startsWith("1");
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


    // TESTME
    private class ReedPollingRunnable implements Runnable {
        private final String TAG = ReedPollingRunnable.class.getSimpleName();
        private final ReedSensor sensor;
        private boolean isOpenFilter = false;

        public ReedPollingRunnable(ReedSensor sensor) {
            this.sensor = sensor;
        }

        @Override
        public void run() {
            try {
                boolean isOpen = sensor.isOpen();
                if (future != null && isOpen != isOpenFilter) {
                    Log.i(TAG, "isOpen(): " + isOpen);
                    isOpenFilter = isOpen;
                    sendReedInfo(isOpen);
                }
            } catch (EvsIoException e) {
                e.printStackTrace();
            }
        }

        /**
         * Sends a message describing whether the reed sensor is opened or closed.
         *
         * @param open true if this reed sensor is open
         */
        private void sendReedInfo(boolean open) {
            MessagePayload payload = new DoorStatusPayload(!open, moduleName);

            NamingManager namingManager = container.require(NamingManager.KEY);

            Message message;
            message = new Message(payload);

            OutgoingRouter router = container.require(OutgoingRouter.KEY);
            router.sendMessage(namingManager.getMasterID(), RoutingKeys.MASTER_DOOR_STATUS_GET, message);
        }
    }

}
