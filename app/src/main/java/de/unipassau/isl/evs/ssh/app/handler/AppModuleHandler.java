package de.unipassau.isl.evs.ssh.app.handler;

import android.util.Log;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
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

    private List<Module> components;
    private List<Slave> slaves;

    public void updateList(List<Module> components, List<Slave> slaves) {
        this.components = components;
        this.slaves = slaves;
    }

    public List<Module> getComponents() {
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

    @Override
    public void handle(Message.AddressedMessage message) {
        String routingKey = message.getRoutingKey();
        if (routingKey.equals(CoreConstants.RoutingKeys.APP_MODULES_GET)) {
            if (message.getPayload() instanceof ModulesPayload) {
                ModulesPayload payload = (ModulesPayload) message.getPayload();
                List<Module> modules = payload.getModules();
                List<Slave> slaves = payload.getSlaves();
                updateList(modules, slaves);
            } else {
                Log.e(this.getClass().getSimpleName(), "Error! Unknown message Payload");
            }

        } else {
            throw new UnsupportedOperationException("Unknown routing key");
        }
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
        requireComponent(IncomingDispatcher.KEY).registerHandler(this, CoreConstants.RoutingKeys.APP_MODULES_GET);
    }

    @Override
    public void destroy() {
        requireComponent(IncomingDispatcher.KEY).unregisterHandler(this, CoreConstants.RoutingKeys.APP_MODULES_GET);
        super.destroy();
    }
}
