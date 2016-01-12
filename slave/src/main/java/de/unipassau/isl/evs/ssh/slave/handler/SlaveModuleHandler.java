package de.unipassau.isl.evs.ssh.slave.handler;

import android.util.Log;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.GPIOAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.MockAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.USBAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.WLANAccessPoint;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.handler.WrongAccessPointException;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
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
    public static final Key<SlaveModuleHandler> KEY = new Key<>(SlaveModuleHandler.class);
    private static final String TAG = SlaveModuleHandler.class.getSimpleName();
    private final List<Module> components = new LinkedList<>();

    /**
     * Updates the SlaveContainer. Registers new modules and unregisters unused modules.
     *
     * @param components a list of modules to add. All modules in the given list are registered in
     *                   the SaveContainer. All modules that are in the container but not in the
     *                   list are unregistered.
     */
    public void updateModule(List<Module> components) throws WrongAccessPointException, EvsIoException {
        if (this.components.size() < 1) {
            final Set<Module> newComponents = Sets.newHashSet(components);
            registerModules(newComponents);
        } else {
            final Set<Module> oldComponents = Sets.newHashSet(this.components);
            final Set<Module> newComponents = Sets.newHashSet(components);
            final Set<Module> componentsToRemove = Sets.difference(oldComponents, newComponents);
            final Set<Module> componentsToAdd = Sets.difference(newComponents, oldComponents);

            unregisterModule(componentsToRemove);
            registerModules(componentsToAdd);
        }
        this.components.clear();
        this.components.addAll(components);
    }

    private void registerModules(Set<Module> componentsToAdd) throws WrongAccessPointException, EvsIoException {
        for (Module module : componentsToAdd) {
            final Class<? extends Component> clazz = getDriverClass(module);
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
        final String moduleName = buttonSensor.getName();
        final GPIOAccessPoint accessPoint = (GPIOAccessPoint) buttonSensor.getModuleAccessPoint();

        if (!buttonSensor.getModuleType().isValidAccessPoint(accessPoint)) {
            throw new WrongAccessPointException();
        }
        assert getContainer() != null;
        final Key<ButtonSensor> key = new Key<>(ButtonSensor.class, moduleName);
        getContainer().register(key, new ButtonSensor(accessPoint.getPort(), moduleName));
    }

    private void registerDoorBuzzer(Module doorBuzzer) throws WrongAccessPointException, EvsIoException {
        final String moduleName = doorBuzzer.getName();
        final GPIOAccessPoint accessPoint = (GPIOAccessPoint) doorBuzzer.getModuleAccessPoint();

        if (!(doorBuzzer.getModuleType().isValidAccessPoint(accessPoint))) {
            throw new WrongAccessPointException();
        }
        assert getContainer() != null;
        final Key<DoorBuzzer> key = new Key<>(DoorBuzzer.class, moduleName);
        getContainer().register(key, new DoorBuzzer(accessPoint.getPort()));
    }

    private void registerReedSensor(Module reedSensor) throws WrongAccessPointException, EvsIoException {
        final String moduleName = reedSensor.getName();
        final GPIOAccessPoint accessPoint = (GPIOAccessPoint) reedSensor.getModuleAccessPoint();

        if (!(reedSensor.getModuleType().isValidAccessPoint(accessPoint))) {
            throw new WrongAccessPointException();
        }
        final Key<ReedSensor> key = new Key<>(ReedSensor.class, moduleName);
        assert getContainer() != null;
        getContainer().register(key, new ReedSensor(accessPoint.getPort(), moduleName));
    }

    private void registerWeatherSensor(Module weatherSensor) throws WrongAccessPointException {
        final String moduleName = weatherSensor.getName();
        final USBAccessPoint accessPoint = (USBAccessPoint) weatherSensor.getModuleAccessPoint();

        if (!weatherSensor.getModuleType().isValidAccessPoint(accessPoint)) {
            throw new WrongAccessPointException();
        }
        assert getContainer() != null;
        final Key<WeatherSensor> key = new Key<>(WeatherSensor.class, moduleName);
        getContainer().register(key, new WeatherSensor(weatherSensor));
    }

    private void registerEdimaxPlugSwitch(Module plugSwitch) throws WrongAccessPointException {
        assert getContainer() != null;

        final String moduleName = plugSwitch.getName();

        if (!plugSwitch.getModuleType().isValidAccessPoint(plugSwitch.getModuleAccessPoint())) {
            throw new WrongAccessPointException();
        }
        if ( plugSwitch.getModuleAccessPoint().getType().equals(WLANAccessPoint.TYPE)) {
            final Key<EdimaxPlugSwitch> key = new Key<>(EdimaxPlugSwitch.class, moduleName);
            final WLANAccessPoint accessPoint = (WLANAccessPoint) plugSwitch.getModuleAccessPoint();
            getContainer().register(key, new EdimaxPlugSwitch(accessPoint.getiPAddress(),
                    accessPoint.getPort(), accessPoint.getUsername(), accessPoint.getPassword()));
        } else if (plugSwitch.getModuleAccessPoint().getType().equals(MockAccessPoint.TYPE)) {
            final Key<EdimaxPlugSwitchMock> key = new Key<>(EdimaxPlugSwitchMock.class, moduleName);
            getContainer().register(key, new EdimaxPlugSwitchMock());
        }
    }

    private void unregisterModule(Set<Module> componentsToRemove) {
        for (Module module : componentsToRemove) {
            final Key<? extends Component> key = new Key<>(getDriverClass(module), module.getName());
            assert getContainer() != null;
            if (getContainer().isRegistered(key)) {
                getContainer().unregister(key);
            }
        }
    }

    public Class<? extends Component> getDriverClass(Module module) {
        final Class<? extends Component> clazz;
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
            default:
                clazz = null;
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
            final ModulesPayload payload = GLOBAL_MODULES_UPDATE.getPayload(message);
            final DeviceID myself = requireComponent(NamingManager.KEY).getOwnID();
            final List<Module> modules = payload.getModulesAtSlave(myself);
            try {
                updateModule(modules);
            } catch (WrongAccessPointException | EvsIoException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{GLOBAL_MODULES_UPDATE};
    }
}
