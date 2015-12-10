package de.unipassau.isl.evs.ssh.slave.handler;

import android.util.Log;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.GPIOAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.WLANAccessPoint;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.drivers.lib.ButtonSensor;
import de.unipassau.isl.evs.ssh.drivers.lib.DoorBuzzer;
import de.unipassau.isl.evs.ssh.drivers.lib.EdimaxPlugSwitch;
import de.unipassau.isl.evs.ssh.drivers.lib.EvsIo;
import de.unipassau.isl.evs.ssh.drivers.lib.EvsIoException;
import de.unipassau.isl.evs.ssh.drivers.lib.ReedSensor;
import de.unipassau.isl.evs.ssh.drivers.lib.WeatherSensor;

/**
 * SlaveModuleHandler offers a list of all Modules that are active in the System and (un)registers
 * modules in the SlaveContainer when they become (un)available.
 *
 * @author bucher
 * @author Wolfgang Popp
 */
public class SlaveModuleHandler extends AbstractComponent implements MessageHandler {
    public static final Key<SlaveModuleHandler> KEY = new Key<>(SlaveModuleHandler.class);

    private static final String TAG = SlaveModuleHandler.class.getSimpleName();
    private List<Module> components = null;

    /**
     * Updates the SlaveContainer. Registers new modules and unregisters unused modules.
     *
     * @param components a list of modules to add. All modules in the given list are registered in
     *                   the SaveContainer. All modules that are in the container but not in the
     *                   list are unregistered.
     */
    public void updateModule(List<Module> components) throws WrongAccessPointException, EvsIoException {
        if (this.components == null) {
            Set<Module> newComponents = Sets.newHashSet(components);
            registerModules(newComponents);
        } else {
            Set<Module> oldComponents = Sets.newHashSet(this.components);
            Set<Module> newComponents = Sets.newHashSet(components);
            Set<Module> componentsToRemove = Sets.difference(oldComponents, newComponents);
            Set<Module> componentsToAdd = Sets.difference(newComponents, oldComponents);

            unregisterModule(componentsToRemove);
            registerModules(componentsToAdd);
        }
        this.components = components;
    }

    private void registerModules(Set<Module> componentsToAdd) throws WrongAccessPointException, EvsIoException {
        for (Module module : componentsToAdd) {
            Class<? extends Component> clazz = getDriverClass(module);
            if (clazz.getName().equals(ButtonSensor.class.getName())) {
                registerButtonSensor(module);
            } else if (clazz.getName().equals(DoorBuzzer.class.getName())) {
                registerDoorBuzzer(module);
            } else if (clazz.getName().equals(ReedSensor.class.getName())) {
                registerReedSensor(module);
            } else if (clazz.getName().equals(WeatherSensor.class.getName())) {
                registerWeatherSensor(module);
            } else if (clazz.getName().equals(EdimaxPlugSwitch.class.getName())) {
                registerEdimaxPlugSwitch(module);
            }
        }
    }

    private void registerButtonSensor(Module buttonSensor) throws WrongAccessPointException, EvsIoException {
        String moduleName = buttonSensor.getName();
        Key<ButtonSensor> key = new Key<>(ButtonSensor.class, moduleName);
        if (!(buttonSensor.getModuleAccessPoint() instanceof GPIOAccessPoint)) {
            throw new WrongAccessPointException();
        }
        GPIOAccessPoint accessPoint = (GPIOAccessPoint) buttonSensor.getModuleAccessPoint();
        getContainer().register(key, new ButtonSensor(accessPoint.getPort(), moduleName));
    }

    private void registerDoorBuzzer(Module doorBuzzer) throws WrongAccessPointException, EvsIoException {
        String moduleName = doorBuzzer.getName();
        Key<DoorBuzzer> key = new Key<>(DoorBuzzer.class, moduleName);
        if (!(doorBuzzer.getModuleAccessPoint() instanceof GPIOAccessPoint)) {
            throw new WrongAccessPointException();
        }
        GPIOAccessPoint accessPoint = (GPIOAccessPoint) doorBuzzer.getModuleAccessPoint();
        getContainer().register(key, new DoorBuzzer(accessPoint.getPort()));
    }

    private void registerReedSensor(Module reedSensor) throws WrongAccessPointException, EvsIoException {
        String moduleName = reedSensor.getName();
        Key<ReedSensor> key = new Key<>(ReedSensor.class, moduleName);
        if (!(reedSensor.getModuleAccessPoint() instanceof GPIOAccessPoint)) {
            throw new WrongAccessPointException();
        }
        GPIOAccessPoint accessPoint = (GPIOAccessPoint) reedSensor.getModuleAccessPoint();
        getContainer().register(key, new ReedSensor(accessPoint.getPort(), moduleName));
    }

    private void registerWeatherSensor(Module weatherSensor) {
        String moduleName = weatherSensor.getName();
        Key<WeatherSensor> key = new Key<>(WeatherSensor.class, moduleName);
        getContainer().register(key, new WeatherSensor());
    }

    private void registerEdimaxPlugSwitch(Module plugSwitch) throws WrongAccessPointException {
        String moduleName = plugSwitch.getName();
        Key<EdimaxPlugSwitch> key = new Key<>(EdimaxPlugSwitch.class, moduleName);

        if (!(plugSwitch.getModuleAccessPoint() instanceof WLANAccessPoint)) {
            throw new WrongAccessPointException();
        }
        WLANAccessPoint accessPoint = (WLANAccessPoint) plugSwitch.getModuleAccessPoint();
        getContainer().register(key, new EdimaxPlugSwitch(accessPoint.getiPAddress(),
                accessPoint.getPort(), accessPoint.getUsername(), accessPoint.getPassword()));
    }

    private void unregisterModule(Set<Module> componentsToRemove) {
        for (Module module : componentsToRemove) {
            Key<? extends Component> key = new Key<>(getDriverClass(module), module.getName());
            getContainer().unregister(key);
        }
    }

    public Class<? extends Component> getDriverClass(Module module) {
        Class clazz = null;
        switch (module.getModuleType()) {
            case CoreConstants.ModuleType.WINDOW_SENSOR:
            case CoreConstants.ModuleType.DOOR_SENSOR:
                clazz = ReedSensor.class;
                break;
            case CoreConstants.ModuleType.WEATHER_BOARD:
                clazz = WeatherSensor.class;
                break;
            case CoreConstants.ModuleType.DOOR_BUZZER:
                clazz = DoorBuzzer.class;
                break;
            case CoreConstants.ModuleType.DOORBELL:
                clazz = ButtonSensor.class;
                break;
            case CoreConstants.ModuleType.LIGHT:
                clazz = EdimaxPlugSwitch.class;
                break;
        }
        return clazz;
    }

    /**
     * Gets a list of currently registered components.
     *
     * @return a list of registered components
     */
    public List<Module> getModules() {
        return new ArrayList<>(components);
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        String routingKey = message.getRoutingKey();
        if (routingKey.equals(CoreConstants.RoutingKeys.MODULES_UPDATE)) {
            if (message.getPayload() instanceof ModulesPayload) {
                ModulesPayload payload = (ModulesPayload) message.getPayload();
                DeviceID myself = requireComponent(NamingManager.KEY).getOwnID();
                List<Module> modules = payload.getModulesAtSlave(myself);
                try {
                    updateModule(modules);
                } catch (WrongAccessPointException | EvsIoException e) {
                    getContainer().require(OutgoingRouter.KEY).sendMessage(message.getFromID(),
                            message.getHeader(Message.HEADER_REPLY_TO_KEY),
                            new Message(new MessageErrorPayload(message.getPayload())));
                }
            } else {
                Log.e(TAG, "Error! Unknown message Payload");
            }
        } else {
            Log.e(TAG, "Error! Unsupported routing key");
        }
    }

    @Override
    public void init(Container container) {
        super.init(container);
        requireComponent(IncomingDispatcher.KEY).registerHandler(this, CoreConstants.RoutingKeys.MODULES_UPDATE);
    }

    @Override
    public void destroy() {
        requireComponent(IncomingDispatcher.KEY).unregisterHandler(this, CoreConstants.RoutingKeys.MODULES_UPDATE);
        super.destroy();
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
    }

    @Override
    public void handlerRemoved(String routingKey) {
    }

    private class WrongAccessPointException extends Exception {
        public WrongAccessPointException() { super(); }
        public WrongAccessPointException(String message) { super(message); }
        public WrongAccessPointException(String message, Throwable cause) { super(message, cause); }
        public WrongAccessPointException(Throwable cause) { super(cause); }
    }
}
