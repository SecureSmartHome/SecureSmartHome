/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
    private final int ioAddress;
    private Container container;
    private ScheduledFuture future;

    /**
     * Constructor of the class representing door and window sensors
     *
     * @param ioAdress where the sensor is connected to the odroid
     */
    public ReedSensor(int ioAdress, String moduleName) throws EvsIoException {
        this.moduleName = moduleName;
        this.ioAddress = ioAdress;
        EvsIo.registerPin(ioAdress, "in");
    }

    /**
     * Checks if the window is open
     *
     * @return true if the window is currently open
     */
    public boolean isOpen() throws EvsIoException {
        return EvsIo.readValue(ioAddress).startsWith("1");
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
        private final String TAG = ReedPollingRunnable.class.getSimpleName();
        private final ReedSensor sensor;
        private boolean isOpenFilter = false;
        private boolean isFirstStart = true;

        public ReedPollingRunnable(ReedSensor sensor) {
            this.sensor = sensor;
        }

        @Override
        public void run() {
            try {
                boolean isOpen = sensor.isOpen();
                if (future != null && (isOpen != isOpenFilter || isFirstStart)) {
                    Log.i(TAG, "isOpen(): " + isOpen);
                    isOpenFilter = isOpen;
                    isFirstStart = false;
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
            MessagePayload payload = new DoorStatusPayload(open, false, moduleName);

            NamingManager namingManager = container.require(NamingManager.KEY);

            Message message;
            message = new Message(payload);

            OutgoingRouter router = container.require(OutgoingRouter.KEY);
            router.sendMessage(namingManager.getMasterID(), RoutingKeys.MASTER_DOOR_STATUS_UPDATE, message);
        }
    }

}
