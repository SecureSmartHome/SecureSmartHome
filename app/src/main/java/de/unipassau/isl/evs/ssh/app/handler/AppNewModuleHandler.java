package de.unipassau.isl.evs.ssh.app.handler;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.AddNewModulePayload;

public class AppNewModuleHandler extends AbstractComponent implements MessageHandler{
    public static final Key<AppNewModuleHandler> KEY = new Key<>(AppNewModuleHandler.class);

    private List<NewModuleListener> listeners = new LinkedList<>();

    public interface NewModuleListener{
        void moduleRegistered();
    }

    public void addNewModuleListener(AppNewModuleHandler.NewModuleListener listener) {
        listeners.add(listener);
    }

    public void removeNewModuleListener(AppNewModuleHandler.NewModuleListener listener) {
        listeners.remove(listener);
    }

    private void fireModulesUpdated(){
        for (NewModuleListener listener : listeners) {
            listener.moduleRegistered();
        }
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        String routingKey = message.getRoutingKey();
        if (routingKey.equals(CoreConstants.RoutingKeys.APP_MODULES_GET)) {
            fireModulesUpdated();
        }

    }

    @Override
    public void init(Container container) {
        super.init(container);
        container.require(IncomingDispatcher.KEY).registerHandler(this, CoreConstants.RoutingKeys.APP_MODULE_ADD);
    }

    @Override
    public void destroy() {
        getComponent(IncomingDispatcher.KEY).unregisterHandler(this, CoreConstants.RoutingKeys.APP_MODULE_ADD);
        super.destroy();
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {

    }

    public void addNewModule(Module module) {
        AddNewModulePayload payload = new AddNewModulePayload(module);

        Message message = new Message(payload);

        OutgoingRouter router = getComponent(OutgoingRouter.KEY);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_MODULE_ADD, message);

    }
}
