package de.unipassau.isl.evs.ssh.slave.handler;

import com.google.common.base.Predicate;

import java.util.List;
import java.util.Objects;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.GPIOAccessPoint;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;

/**
 * SlaveModulHandler offers a list of all Modules that are active in the System.
 * @author bucher
 */
public class SlaveModulHandler implements MessageHandler{
    public static final Predicate<Module> COMPONENTS_IN_ODROID = new Predicate<Module>() {
        @Override
        public boolean apply(Module input) {
            return false;
            //Objects.equals(input.getAtSlave().getId(), require("KeyStoreManager").getLocalID());
        }
    };

    private OutgoingRouter outgoing;
    private Container container;
    private IncomingDispatcher incomingDispatcher;
    private List<Module> components;

    public void UpdateModule(List<Module> components){
        this.components = components;

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
