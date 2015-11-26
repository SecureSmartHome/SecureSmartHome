package de.unipassau.isl.evs.ssh.app;

import com.google.common.base.Predicate;

import java.util.List;
import java.util.Objects;

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
    public static final Predicate<Module> PREDICATE_GPIO = new Predicate<Module>() {
        @Override
        public boolean apply(Module input) {
            return Objects.equals(input.getModuleAccessPoint().getType(), GPIOAccessPoint.TYPE);
        }
    };

    private List<Module> components;
    private List<Module> lights;
    private List<Module> sensors;

    public void UpdateList(List<Module> components) {
        this.components = components;
//        while (!components.isEmpty()) {
//            String type = components.get(0).getModuleAccessPoint().getType();
//            Module firstInList = components.get(0);
//            components.remove(0);
//            switch (type) {
//                case GPIOAccessPoint.TYPE:
//                    sensors.add(firstInList);
//                case USBAccessPoint.TYPE:
//                    //USB only in camera?
//                case WLANAccessPoint.TYPE:
//                    lights.add(firstInList);
//                default:
//                    throw new IllegalArgumentException("Type " + type + "is not known.");
//            }
//        }
    }


    public List<Module> getComponents() {
        return components;
    }

//    public List<Module> getLights() {
//        return lights;
//    }

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
