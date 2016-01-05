package de.unipassau.isl.evs.ssh.slave.handler;

import android.util.Log;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.GPIOAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.MockAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.WLANAccessPoint;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.drivers.lib.ButtonSensor;
import de.unipassau.isl.evs.ssh.drivers.lib.DoorBuzzer;
import de.unipassau.isl.evs.ssh.drivers.lib.EdimaxPlugSwitch;
import de.unipassau.isl.evs.ssh.drivers.lib.EvsIoException;
import de.unipassau.isl.evs.ssh.drivers.lib.ReedSensor;
import de.unipassau.isl.evs.ssh.drivers.lib.WeatherSensor;
import de.unipassau.isl.evs.ssh.drivers.mock.EdimaxPlugSwitchMock;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.GLOBAL_MODULES_UPDATE;

/**
 * SlaveModuleHandler offers a list of all Modules that are active in the System and (un)registers
 * modules in the SlaveContainer when they become (un)available.
 *
 * @author Andreas Bucher
 * @author Wolfgang Popp
 */
public class SlaveModuleHandler extends AbstractMessageHandler implements Component {
    private static final String TAG = SlaveModuleHandler.class.getSimpleName();
    public static final Key<SlaveModuleHandler> KEY = new Key<>(SlaveModuleHandler.class);

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
            if (clazz == null) {
                Log.i(TAG, "Module has unknown type/accesspoint combination");
                return;
            }

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
        assert getContainer() != null;
        getContainer().register(key, new ButtonSensor(accessPoint.getPort(), moduleName));
    }

    private void registerDoorBuzzer(Module doorBuzzer) throws WrongAccessPointException, EvsIoException {
        String moduleName = doorBuzzer.getName();
        Key<DoorBuzzer> key = new Key<>(DoorBuzzer.class, moduleName);
        if (!(doorBuzzer.getModuleAccessPoint() instanceof GPIOAccessPoint)) {
            throw new WrongAccessPointException();
        }
        GPIOAccessPoint accessPoint = (GPIOAccessPoint) doorBuzzer.getModuleAccessPoint();
        assert getContainer() != null;
        getContainer().register(key, new DoorBuzzer(accessPoint.getPort()));
    }

    private void registerReedSensor(Module reedSensor) throws WrongAccessPointException, EvsIoException {
        String moduleName = reedSensor.getName();
        Key<ReedSensor> key = new Key<>(ReedSensor.class, moduleName);
        if (!(reedSensor.getModuleAccessPoint() instanceof GPIOAccessPoint)) {
            throw new WrongAccessPointException();
        }
        GPIOAccessPoint accessPoint = (GPIOAccessPoint) reedSensor.getModuleAccessPoint();
        assert getContainer() != null;
        getContainer().register(key, new ReedSensor(accessPoint.getPort(), moduleName));
    }

    private void registerWeatherSensor(Module weatherSensor) {
        String moduleName = weatherSensor.getName();
        Key<WeatherSensor> key = new Key<>(WeatherSensor.class, moduleName);
        assert getContainer() != null;
        getContainer().register(key, new WeatherSensor());
    }

    private void registerEdimaxPlugSwitch(Module plugSwitch) throws WrongAccessPointException {
        assert getContainer() != null;

        String moduleName = plugSwitch.getName();
        Key<EdimaxPlugSwitch> key = new Key<>(EdimaxPlugSwitch.class, moduleName);

        if (plugSwitch.getModuleAccessPoint() instanceof WLANAccessPoint) {
            WLANAccessPoint accessPoint = (WLANAccessPoint) plugSwitch.getModuleAccessPoint();
            getContainer().register(key, new EdimaxPlugSwitch(accessPoint.getiPAddress(),
                    accessPoint.getPort(), accessPoint.getUsername(), accessPoint.getPassword()));
        } else if (plugSwitch.getModuleAccessPoint() instanceof MockAccessPoint) {
            getContainer().register(key, new EdimaxPlugSwitchMock());
        } else {
            throw new WrongAccessPointException();
        }
    }

    private void unregisterModule(Set<Module> componentsToRemove) {
        for (Module module : componentsToRemove) {
            Key<? extends Component> key = new Key<>(getDriverClass(module), module.getName());
            assert getContainer() != null;
            getContainer().unregister(key);
        }
    }

    public Class<? extends Component> getDriverClass(Module module) {
        Class<? extends Component> clazz = null;
        switch (module.getModuleType()) {
            case WindowSensor:
            case DoorSensor:
                clazz = ReedSensor.class;
                break;
            case WeatherBoard:
                clazz = WeatherSensor.class;
                break;
            case DoorBuzzer:
                clazz = DoorBuzzer.class;
                break;
            case Doorbell:
                clazz = ButtonSensor.class;
                break;
            case Light:
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
        if (GLOBAL_MODULES_UPDATE.matches(message)) {
            ModulesPayload payload = GLOBAL_MODULES_UPDATE.getPayload(message);
            DeviceID myself = requireComponent(NamingManager.KEY).getOwnID();
            List<Module> modules = payload.getModulesAtSlave(myself);
            try {
                updateModule(modules);
            } catch (WrongAccessPointException | EvsIoException e) {
                // HANDLE
                Log.e(TAG, "Could not update Modules from payload " + payload, e);
                sendMessage(
                        message.getFromID(),
                        message.getHeader(Message.HEADER_REPLY_TO_KEY),
                        new Message(new MessageErrorPayload(message.getPayload()))
                );
            }
        } else {
            // HANDLE
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{GLOBAL_MODULES_UPDATE};
    }

    private class WrongAccessPointException extends Exception {
        public WrongAccessPointException() {
            super();
        }

        public WrongAccessPointException(String message) {
            super(message);
        }

        public WrongAccessPointException(String message, Throwable cause) {
            super(message, cause);
        }

        public WrongAccessPointException(Throwable cause) {
            super(cause);
        }
    }
}
