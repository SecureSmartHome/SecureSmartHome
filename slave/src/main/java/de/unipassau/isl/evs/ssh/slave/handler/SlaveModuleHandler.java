package de.unipassau.isl.evs.ssh.slave.handler;

import android.util.Log;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;

/**
 * SlaveModuleHandler offers a list of all Modules that are active in the System.
 *
 * @author bucher
 */
public class SlaveModuleHandler extends AbstractComponent implements MessageHandler {
    public static final Key<SlaveModuleHandler> KEY = new Key<>(SlaveModuleHandler.class);

    private List<Module> components;

    public void updateModule(List<Module> components) {
        //TODO update container
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
