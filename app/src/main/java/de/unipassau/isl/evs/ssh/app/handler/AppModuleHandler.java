package de.unipassau.isl.evs.ssh.app.handler;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.APP_MODULES_GET;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.GLOBAL_MODULES_UPDATE;

/**
 * AppModuleHandler offers a list of all Modules that are active in the System.
 *
 * @author Andreas Bucher
 */
public class AppModuleHandler extends AbstractMessageHandler implements Component {
    public static final Key<AppModuleHandler> KEY = new Key<>(AppModuleHandler.class);

    /**
     * Use sample code to filter for specific components
     * <pre>
     * Iterable<Module> filtered = Iterables.filter(components, PREDICATE_GPIO);
     * ArrayList<Module> modules = Lists.newArrayList(filtered);
     * </pre>
     */
    private static final Predicate<Module> PREDICATE_LIGHT = new Predicate<Module>() {
        @Override
        public boolean apply(Module input) {
            return Objects.equals(input.getModuleType(), CoreConstants.ModuleType.LIGHT);
        }
    };

    private static final Predicate<Module> PREDICATE_DOOR = new Predicate<Module>() {
        @Override
        public boolean apply(Module input) {
            return Objects.equals(input.getModuleType(), CoreConstants.ModuleType.DOOR_SENSOR);
        }
    };

    private static final Predicate<Module> PREDICATE_WEATHER = new Predicate<Module>() {
        @Override
        public boolean apply(Module input) {
            return Objects.equals(input.getModuleType(), CoreConstants.ModuleType.WEATHER_BOARD);
        }
    };

    private static final Predicate<Module> PREDICATE_CAMERA = new Predicate<Module>() {
        @Override
        public boolean apply(Module input) {
            return Objects.equals(input.getModuleType(), CoreConstants.ModuleType.WEBCAM);
        }
    };

    private Set<Module> components;
    private List<Slave> slaves;
    private ListMultimap<Slave, Module> modulesAtSlave;

    private void updateList(Set<Module> components, List<Slave> slaves, ListMultimap<Slave, Module> modulesAtSlave) {
        this.components = components;
        this.slaves = slaves;
        this.modulesAtSlave = modulesAtSlave;
    }

    public Set<Module> getComponents() {
        return components;
    }

    public List<Module> getLights() {
        if (getComponents() == null) {
            return null;
        }
        Iterable<Module> filtered = Iterables.filter(components, PREDICATE_LIGHT);
        return Lists.newArrayList(filtered);
    }

    public List<Module> getDoors() {
        if (getComponents() == null) {
            return null;
        }
        Iterable<Module> filtered = Iterables.filter(components, PREDICATE_DOOR);
        return Lists.newArrayList(filtered);
    }

    public List<Module> getCameras() {
        if (getComponents() == null) {
            return null;
        }
        Iterable<Module> filtered = Iterables.filter(components, PREDICATE_CAMERA);
        return Lists.newArrayList(filtered);
    }

    public List<Module> getWeather() {
        if (getComponents() == null) {
            return null;
        }
        Iterable<Module> filtered = Iterables.filter(components, PREDICATE_WEATHER);
        return Lists.newArrayList(filtered);
    }

    public List<Slave> getSlaves() {
        if (getComponents() == null) {
            return null;
        }
        return ImmutableList.copyOf(slaves);
    }

    /**
     * Returns a list of connected modules at the given slave.
     *
     * @param slave the slave
     * @return a list of connected modules at the given slave
     */
    public List<Module> getModulesAtSlave(Slave slave) {
        return modulesAtSlave.get(slave);
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (APP_MODULES_GET.matches(message) || GLOBAL_MODULES_UPDATE.matches(message)) {
            ModulesPayload payload = message.getPayloadOfClass(ModulesPayload.class);
            Set<Module> modules = payload.getModules();
            List<Slave> slaves = payload.getSlaves();
            ListMultimap<Slave, Module> modulesAtSlave = payload.getModulesAtSlaves();
            updateList(modules, slaves, modulesAtSlave);
            //Todo: don't do this. do the thing that really needs to be done. this is just here because it's working for now!
            for (Module module : getLights()) {
                requireComponent(AppLightHandler.KEY).setLight(module, false);
            }
        } else {
            invalidMessage(message);
        }
    }

    private void update() {
        ModulesPayload payload = new ModulesPayload();
        OutgoingRouter router = getComponent(OutgoingRouter.KEY);

        Message message = new Message(payload);

        message.putHeader(Message.HEADER_REPLY_TO_KEY, APP_MODULES_GET.getKey());
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_MODULE_GET, message);
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{APP_MODULES_GET, GLOBAL_MODULES_UPDATE};
    }
}
