package de.unipassau.isl.evs.ssh.app;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.GPIOAccessPoint;
//import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.USBAccessPoint;
//import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.WLANAccessPoint;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * AppModuleHandler offers a list of all Modules that are active in the System.
 * @author bucher
 */
public class AppModuleHandler implements MessageHandler {
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

    public void UpdateList(List<Module> components) {
        this.components = components;
    }


    public List<Module> getComponents() {
        return components;
    }

    public List<Module> getLights(){
         Iterable<Module> filtered = Iterables.filter(components, PREDICATE_LIGHT);
         ArrayList<Module> lights = Lists.newArrayList(filtered);
        return lights;
    }

    public List<Module> getDoors(){
        Iterable<Module> filtered = Iterables.filter(components, PREDICATE_DOOR);
        ArrayList<Module> doors = Lists.newArrayList(filtered);
        return doors;
    }

    public List<Module> getWeather(){
        Iterable<Module> filtered = Iterables.filter(components, PREDICATE_WEATHER);
        ArrayList<Module> weather = Lists.newArrayList(filtered);
        return weather;
    }

    @Override
    public void handle(Message.AddressedMessage message) {
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
    }

    @Override
    public void handlerRemoved(String routingKey) {
    }
}
