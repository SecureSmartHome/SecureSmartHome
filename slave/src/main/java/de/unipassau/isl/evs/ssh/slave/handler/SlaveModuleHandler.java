package de.unipassau.isl.evs.ssh.slave.handler;

import android.util.Log;

import com.google.common.collect.Sets;

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
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;
import de.unipassau.isl.evs.ssh.drivers.lib.ButtonSensor;
import de.unipassau.isl.evs.ssh.drivers.lib.DoorBuzzer;
import de.unipassau.isl.evs.ssh.drivers.lib.EdimaxPlugSwitch;
import de.unipassau.isl.evs.ssh.drivers.lib.ReedSensor;
import de.unipassau.isl.evs.ssh.drivers.lib.WeatherSensor;

/**
 * SlaveModuleHandler offers a list of all Modules that are active in the System.
 *
 * @author bucher
 */
public class SlaveModuleHandler extends AbstractComponent implements MessageHandler {
    private static final String TAG = SlaveModuleHandler.class.getSimpleName();
    public static final Key<SlaveModuleHandler> KEY = new Key<>(SlaveModuleHandler.class);

    private List<Module> components;

    public void updateModule(List<Module> components) {
        Set<Module> oldComponents = Sets.newHashSet(this.components);
        Set<Module> newComponents = Sets.newHashSet(components);

        Set<Module> componentsToRemove = Sets.difference(oldComponents, newComponents);
        Set<Module> componentsToAdd = Sets.difference(newComponents, oldComponents);

        unregisterModule(componentsToRemove);
        registerModules(componentsToAdd);
        this.components = components;
    }

    private void registerModules(Set<Module> componentsToAdd) {
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

    private void registerButtonSensor(Module buttonSensor) {
        String moduleName = buttonSensor.getName();
        Key<ButtonSensor> key = new Key<>(ButtonSensor.class, moduleName);
        GPIOAccessPoint accessPoint = (GPIOAccessPoint) buttonSensor.getModuleAccessPoint();
        getContainer().register(key, new ButtonSensor(accessPoint.getPort(), moduleName));
    }

    private void registerDoorBuzzer(Module doorBuzzer) {
        String moduleName = doorBuzzer.getName();
        Key<DoorBuzzer> key = new Key<>(DoorBuzzer.class, moduleName);
        GPIOAccessPoint accessPoint = (GPIOAccessPoint) doorBuzzer.getModuleAccessPoint();
        getContainer().register(key, new DoorBuzzer(accessPoint.getPort()));
    }

    private void registerReedSensor(Module reedSensor) {
        String moduleName = reedSensor.getName();
        Key<ReedSensor> key = new Key<>(ReedSensor.class, moduleName);
        GPIOAccessPoint accessPoint = (GPIOAccessPoint) reedSensor.getModuleAccessPoint();
        getContainer().register(key, new ReedSensor(accessPoint.getPort()));
    }

    private void registerWeatherSensor(Module weatherSensor) {
        String moduleName = weatherSensor.getName();
        Key<WeatherSensor> key = new Key<>(WeatherSensor.class, moduleName);
        getContainer().register(key, new WeatherSensor());
    }

    private void registerEdimaxPlugSwitch(Module plugSwitch) {
        String moduleName = plugSwitch.getName();
        Key<EdimaxPlugSwitch> key = new Key<>(EdimaxPlugSwitch.class, moduleName);
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

    private Class<? extends Component> getDriverClass(Module module) {
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

    public List<Module> getComponents() {
        return components;
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        String routingKey = message.getRoutingKey();
        if (routingKey.equals(CoreConstants.RoutingKeys.SLAVE_MODULES_UPDATE)) {
            if (message.getPayload() instanceof ModulesPayload) {
                List<Module> modules = (List<Module>) message.getPayload();
                updateModule(modules);
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
        requireComponent(IncomingDispatcher.KEY).registerHandler(this, CoreConstants.RoutingKeys.SLAVE_MODULES_UPDATE);
    }

    @Override
    public void destroy() {
        requireComponent(IncomingDispatcher.KEY).unregisterHandler(this, CoreConstants.RoutingKeys.SLAVE_MODULES_UPDATE);
        super.destroy();
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
    }

    @Override
    public void handlerRemoved(String routingKey) {
    }
}
