package de.unipassau.isl.evs.ssh.app;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.GPIOAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.USBAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.WLANAccessPoint;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * @author bucher
 */
public class AppModuleHandler implements MessageHandler {
    private List<Module> components;
    private List<Module> lights;
    private List<Module> sensors;

    public void UpdateList(List<Module> components) {
        this.components = components;
        //send received message?
        while (!components.isEmpty()) {
            String type = components.get(0).getModuleAccessPoint().getType();
            Module firstInList = components.get(0);
            components.remove(0);
            switch (type) {
                case GPIOAccessPoint.TYPE:
                    sensors.add(firstInList);
                case USBAccessPoint.TYPE:
                    //USB only in camera?
                case WLANAccessPoint.TYPE:
                    lights.add(firstInList);
                default:
                    throw new IllegalArgumentException("Type " + type + "is not known.");
            }
        }
    }

    public List getComponents() {
        return components;
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
