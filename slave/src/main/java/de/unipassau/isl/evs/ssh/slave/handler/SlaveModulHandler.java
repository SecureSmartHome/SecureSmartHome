package de.unipassau.isl.evs.ssh.slave.handler;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;

/**
 * @author bucher
 */
public class SlaveModulHandler implements MessageHandler{
    private OutgoingRouter outgoing;
    private Container container;
    private IncomingDispatcher incomingDispatcher;
    private List<Module> components;

    public SlaveModulHandler(){
    }

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
