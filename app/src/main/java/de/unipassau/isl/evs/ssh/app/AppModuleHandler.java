package de.unipassau.isl.evs.ssh.app;

import android.util.Log;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
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

    private List<Module> components;

    public void updateList(List<Module> components) {
        this.components = components;
    }


    public List<Module> getComponents() {
        return components;
    }

    public List<Module> getLights() {
        Iterable<Module> filtered = Iterables.filter(components, PREDICATE_LIGHT);
        return Lists.newArrayList(filtered);
    }

    public List<Module> getDoors() {
        Iterable<Module> filtered = Iterables.filter(components, PREDICATE_DOOR);
        return Lists.newArrayList(filtered);
    }

    public List<Module> getWeather() {
        Iterable<Module> filtered = Iterables.filter(components, PREDICATE_WEATHER);
        return Lists.newArrayList(filtered);
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof ModulesPayload) {
            List<Module> modules = (List<Module>) message.getPayload();
            updateList(modules);
        } else {
            Log.e(this.getClass().getSimpleName(), "Error! Unknown message Payload");
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
