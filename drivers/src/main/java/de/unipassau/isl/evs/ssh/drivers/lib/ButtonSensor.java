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
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;

/**
 * Class to get the state of the push button
 *
 * @author Wolfram Gottschlich
 * @version 0.1
 */

public class ButtonSensor extends AbstractComponent {
    public static final Key<ButtonSensor> KEY = new Key<>(ButtonSensor.class);
    private final String moduleName;
    int address;
    int dummyCount;
    private Container container;
    private ScheduledFuture future;

    /**
     * Constructor of the class representing a push button
     *
     * @param IoAdress where the button is connected to the odroid
     */
    public ButtonSensor(int IoAdress, String moduleName) throws EvsIoException {
        this.moduleName = moduleName;
        address = IoAdress;
        EvsIo.registerPin(IoAdress, "in");
    }

    /**
     * Checks if the push button is currently pressed
     *
     * @return true if the push button is currently pressed
     */
    public boolean isPressed() throws EvsIoException {
        boolean ret = true;
        String result = "";
        result = EvsIo.readValue(address);
        //Log.v(TAG, "EVS-Button:" + result + ":");


        if (result.startsWith("1")) {
            ret = false;
        } else {
            ret = true;
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
        private final String TAG = DoorPollingRunnable.class.getSimpleName();

        private ButtonSensor sensor;
        private boolean isPressedFilter = false;

        public DoorPollingRunnable(ButtonSensor sensor) {
            this.sensor = sensor;
        }

        @Override
        public void run() {
            try {
                if (future != null && sensor.isPressed() && !isPressedFilter) {
                    Log.i(TAG, "Button Pressed!");
                    sendDoorBellInfo();
                    isPressedFilter = true;
                } else if (!sensor.isPressed()) {
                    isPressedFilter = false;
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

            OutgoingRouter router = container.require(OutgoingRouter.KEY);
            router.sendMessage(namingManager.getMasterID(), RoutingKeys.MASTER_DOOR_BELL_RING, message);
        }
    }
}
