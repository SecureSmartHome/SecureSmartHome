package de.unipassau.isl.evs.ssh.slave.handler;

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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
                Key<? extends Component> key = new Key<>(handler.getDriverClass(module), module.getName());
                checkStatus(handler.getDriverClass(module), requireComponent(key), module);
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
            }//TODO Camera

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
