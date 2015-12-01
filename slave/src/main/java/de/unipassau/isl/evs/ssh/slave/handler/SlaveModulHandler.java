package de.unipassau.isl.evs.ssh.slave.handler;

import android.util.Log;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;

/**
 * SlaveModulHandler offers a list of all Modules that are active in the System.
 *
 * @author bucher
 */
public class SlaveModulHandler implements MessageHandler {

    private List<Module> components;

    public void updateModule(List<Module> components) {
        this.components = components;
    }

    public List<Module> getComponents() {
        return components;
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof ModulesPayload) {
            List<Module> modules = (List<Module>) message.getPayload();
            updateModule(modules);
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
}
