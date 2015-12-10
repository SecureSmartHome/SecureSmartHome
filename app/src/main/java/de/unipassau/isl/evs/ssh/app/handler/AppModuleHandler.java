package de.unipassau.isl.evs.ssh.app.handler;

import android.util.Log;

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
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;

/**
 * AppModuleHandler offers a list of all Modules that are active in the System.
 *
 * @author bucher
 */
public class AppModuleHandler extends AbstractComponent implements MessageHandler {
    public static final Key<AppModuleHandler> KEY = new Key<>(AppModuleHandler.class);

    /**
     * Use sample code to filter for specific components
     * <pre>
     * Iterable<Module> filtered = Iterables.filter(components, PREDICATE_GPIO);
     * ArrayList<Module> modules = Lists.newArrayList(filtered);
     * </pre>
     */
    public static final Predicate<Module> PREDICATE_LIGHT = new Predicate<Module>() {
        @Override
        public boolean apply(Module input) {
            return Objects.equals(input.getModuleType(), CoreConstants.ModuleType.LIGHT);
        }
    };

    public static final Predicate<Module> PREDICATE_DOOR = new Predicate<Module>() {
        @Override
        public boolean apply(Module input) {
            return Objects.equals(input.getModuleType(), CoreConstants.ModuleType.DOOR_SENSOR);
        }
    };

    public static final Predicate<Module> PREDICATE_WEATHER = new Predicate<Module>() {
        @Override
        public boolean apply(Module input) {
            return Objects.equals(input.getModuleType(), CoreConstants.ModuleType.WEATHER_BOARD);
        }
    };

    public static final Predicate<Module> PREDICATE_CAMERA = new Predicate<Module>() {
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

    public List<Module> getModulesAtSlave(Slave slave) {
        return modulesAtSlave.get(slave);
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        String routingKey = message.getRoutingKey();
        if (routingKey.equals(CoreConstants.RoutingKeys.APP_MODULES_GET)
                || routingKey.equals(CoreConstants.RoutingKeys.MODULES_UPDATE)) {

            if (message.getPayload() instanceof ModulesPayload) {
                ModulesPayload payload = (ModulesPayload) message.getPayload();
                Set<Module> modules = payload.getModules();
                List<Slave> slaves = payload.getSlaves();
                ListMultimap<Slave, Module> modulesAtSlave = payload.getModulesAtSlaves();
                updateList(modules, slaves, modulesAtSlave);
                //Todo: don't do this. do the thing that really needs to be done. this is just here because it's working for now!
                for (Module module : getLights()) {
                    requireComponent(AppLightHandler.KEY).setLight(module, false);
                }
            } else {
                Log.e(this.getClass().getSimpleName(), "Error! Unknown message Payload");
            }
        } else {
            throw new UnsupportedOperationException("Unknown routing key");
        }
    }

    public void update() {
        ModulesPayload payload = new ModulesPayload();
        OutgoingRouter router = getComponent(OutgoingRouter.KEY);

        Message message = new Message(payload);

        message.putHeader(Message.HEADER_REPLY_TO_KEY, CoreConstants.RoutingKeys.APP_MODULES_GET);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_MODULE_GET, message);
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
        requireComponent(IncomingDispatcher.KEY).registerHandler(this, CoreConstants.RoutingKeys.APP_MODULES_GET,
                CoreConstants.RoutingKeys.MODULES_UPDATE);
    }

    @Override
    public void destroy() {
        requireComponent(IncomingDispatcher.KEY).unregisterHandler(this, CoreConstants.RoutingKeys.APP_MODULES_GET,
                CoreConstants.RoutingKeys.MODULES_UPDATE);
        super.destroy();
    }
}
