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

package de.unipassau.isl.evs.ssh.slave.handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SystemHealthPayload;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
import de.unipassau.isl.evs.ssh.drivers.lib.ButtonSensor;
import de.unipassau.isl.evs.ssh.drivers.lib.EdimaxPlugSwitch;
import de.unipassau.isl.evs.ssh.drivers.lib.EvsIoException;
import de.unipassau.isl.evs.ssh.drivers.lib.ReedSensor;

/**
 * SlaveHandler that checks periodically if ElectronicModules are still alive.
 * If a module is not alive or reachable a Message with a SystemHealthPayload will be sent to the master.
 *
 * @author Christoph Fraedrich
 */
public class SlaveSystemHealthChecker extends AbstractComponent {
    public static final Key<SlaveSystemHealthChecker> KEY = new Key<>(SlaveSystemHealthChecker.class);
    private static final long UPDATE_TIMER = 45;
    private final Map<Module, Long> failedModules = new HashMap<>();
    private ScheduledFuture future;

    @Override
    public void init(Container container) {
        super.init(container);
        future = container.require(ExecutionServiceComponent.KEY).scheduleAtFixedRate(
                new SystemHealthRunnable(), 0, 1, TimeUnit.MINUTES
        );
    }

    @Override
    public void destroy() {
        future.cancel(true);
        super.destroy();
    }

    private class SystemHealthRunnable implements Runnable {
        @Override
        public void run() {
            SlaveModuleHandler handler = requireComponent(SlaveModuleHandler.KEY);
            List<Module> modules = handler.getModules();

            for (Module module : modules) {
                final Class<? extends Component> driverClass = SlaveModuleHandler.getDriverClass(module);

                if (driverClass != null) {
                    Key<? extends Component> key = new Key<>(driverClass, module.getName());
                    checkStatus(driverClass, requireComponent(key), module);
                }
            }
        }

        private void checkStatus(Class clazz, Component driver, Module module) {
            boolean success = true;

            if (ButtonSensor.class.equals(clazz)) {
                try {
                    ((ButtonSensor) driver).isPressed();
                } catch (EvsIoException e) {
                    success = false;
                }
            } else if (ReedSensor.class.equals(clazz)) {
                try {
                    ((ReedSensor) driver).isOpen();
                } catch (EvsIoException e) {
                    success = false;
                }
            } else if (EdimaxPlugSwitch.class.equals(clazz)) {
                try {
                    ((EdimaxPlugSwitch) driver).isOn();
                } catch (IOException e) {
                    success = false;
                }
            }

            if (success) {
                if (failedModules.containsKey(module)) {
                    failedModules.remove(module);
                    sendUpdateMessage(false, module);
                }
            } else {
                //Check for availability failed, send error message
                if (!failedModules.containsKey(module) ||
                        failedModules.get(module) - System.currentTimeMillis() > TimeUnit.MINUTES.toMillis(UPDATE_TIMER)) {

                    sendUpdateMessage(true, module);
                    failedModules.put(module, System.currentTimeMillis());
                }
            }

        }

        private void sendUpdateMessage(boolean failure, Module module) {
            Message message = new Message(new SystemHealthPayload(failure, module));
            requireComponent(OutgoingRouter.KEY).sendMessageToMaster(
                    RoutingKeys.MASTER_SYSTEM_HEALTH_CHECK, message);
        }
    }
}
