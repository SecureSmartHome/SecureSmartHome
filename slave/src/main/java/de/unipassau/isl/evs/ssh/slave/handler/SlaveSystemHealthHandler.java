package de.unipassau.isl.evs.ssh.slave.handler;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SystemHealthPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.WeatherPayload;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
import de.unipassau.isl.evs.ssh.drivers.lib.ButtonSensor;
import de.unipassau.isl.evs.ssh.drivers.lib.DoorBuzzer;
import de.unipassau.isl.evs.ssh.drivers.lib.EdimaxPlugSwitch;
import de.unipassau.isl.evs.ssh.drivers.lib.EvsIoException;
import de.unipassau.isl.evs.ssh.drivers.lib.ReedSensor;
import de.unipassau.isl.evs.ssh.drivers.lib.WeatherSensor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * SlaveHandler that checkes periodically if ElectronicModules are still alive.
 * If a module is not alive or reachable a Message with a SystemHealthPayload will be sent to the master.
 *
 * @author Chris
 */
public class SlaveSystemHealthHandler extends AbstractComponent implements MessageHandler {
    //TODO maybe refactor to task instead of handler. So far we do not answer messages
    public static final Key<SlaveSystemHealthHandler> KEY = new Key<>(SlaveSystemHealthHandler.class);
    private static final String TAG = SlaveSystemHealthHandler.class.getSimpleName();

    private Container container;
    private ScheduledFuture future;

    @Override
    public void handle(Message.AddressedMessage message) {

    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {

    }

    @Override
    public void init(Container container) {
        super.init(container);
        this.container = container;

        future = container.require(ExecutionServiceComponent.KEY).scheduleAtFixedRate(
                new SystemHealthRunnable(), 0, 60000, TimeUnit.MILLISECONDS
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
                Class clazz = handler.getDriverClass(module);
                Key key = new Key(clazz, module.getName());
                checkStatus(clazz, getContainer().require(key), module);
            }
        }

        private void checkStatus(Class clazz, Component driver, Module module) {
            if (clazz.getName().equals(ButtonSensor.class.getName())) {
                try {
                    ((ButtonSensor) driver).isPressed();
                } catch (EvsIoException e) {
                    Message message = new Message(new SystemHealthPayload(true, module));
                    getContainer().require(OutgoingRouter.KEY).sendMessageToMaster(
                            CoreConstants.RoutingKeys.MASTER_SYSTEM_HEALTH_CHECK, message);
                }
            } else if (clazz.getName().equals(ReedSensor.class.getName())) {
                try {
                    ((ReedSensor) driver).isOpen();
                } catch (EvsIoException e) {
                    Message message = new Message(new SystemHealthPayload(true, module));
                    getContainer().require(OutgoingRouter.KEY).sendMessageToMaster(
                            CoreConstants.RoutingKeys.MASTER_SYSTEM_HEALTH_CHECK, message);
                }
            } else if (clazz.getName().equals(EdimaxPlugSwitch.class.getName())) {
                try {
                    ((EdimaxPlugSwitch) driver).isOn();
                } catch (IOException e) {
                    Message message = new Message(new SystemHealthPayload(true, module));
                    getContainer().require(OutgoingRouter.KEY).sendMessageToMaster(
                            CoreConstants.RoutingKeys.MASTER_SYSTEM_HEALTH_CHECK, message);
                }
            }
        }
    }
}
