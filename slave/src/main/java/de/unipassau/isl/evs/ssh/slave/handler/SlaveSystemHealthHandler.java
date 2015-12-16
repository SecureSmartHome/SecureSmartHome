package de.unipassau.isl.evs.ssh.slave.handler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
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
public class SlaveSystemHealthHandler extends AbstractMessageHandler implements Component {
    //TODO maybe refactor to task instead of handler. So far we do not answer messages
    public static final Key<SlaveSystemHealthHandler> KEY = new Key<>(SlaveSystemHealthHandler.class);

    private ScheduledFuture future;

    @Override
    public void handle(Message.AddressedMessage message) {
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[0];
    }

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
            SlaveModuleHandler handler = getContainer().require(SlaveModuleHandler.KEY);
            List<Module> modules = handler.getModules();
            for (Module module : modules) {
                Key<? extends Component> key = new Key<>(handler.getDriverClass(module), module.getName());
                checkStatus(handler.getDriverClass(module), getContainer().require(key), module);
            }
        }

        private void checkStatus(Class clazz, Component driver, Module module) {
            if (ButtonSensor.class.equals(clazz)) {
                try {
                    ((ButtonSensor) driver).isPressed();
                } catch (EvsIoException e) {
                    Message message = new Message(new SystemHealthPayload(true, module));
                    sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_SYSTEM_HEALTH_CHECK, message);
                }
            } else if (ReedSensor.class.equals(clazz)) {
                try {
                    ((ReedSensor) driver).isOpen();
                } catch (EvsIoException e) {
                    Message message = new Message(new SystemHealthPayload(true, module));
                    sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_SYSTEM_HEALTH_CHECK, message);
                }
            } else if (EdimaxPlugSwitch.class.equals(clazz)) {
                try {
                    ((EdimaxPlugSwitch) driver).isOn();
                } catch (IOException e) {
                    Message message = new Message(new SystemHealthPayload(true, module));
                    sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_SYSTEM_HEALTH_CHECK, message);
                }
            }
        }
    }
}
