package de.unipassau.isl.evs.ssh.app.handler;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.AddNewSensorPayload;

public class AppNewModuleHandler extends AbstractComponent implements MessageHandler{
    public static final Key<AppNewModuleHandler> KEY = new Key<>(AppNewModuleHandler.class);

    @Override
    public void handle(Message.AddressedMessage message) {

    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {

    }

    public void addNewModule(Module module) {
        AddNewSensorPayload payload = new AddNewSensorPayload(module);

        Message message = new Message(payload);

        OutgoingRouter router = getComponent(OutgoingRouter.KEY);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_MODULE_ADD, message);

    }
}
