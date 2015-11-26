package de.unipassau.isl.evs.ssh.master.handler;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

/**
 * The MasterModuleHandler sends updated lists of active Modules to ODROIDs and Clients
 * @author bucher
 */
public class MasterModuleHandler implements MessageHandler {
    private SlaveController slaveController;
    private OutgoingRouter outgoing;
    private Container container;
    private IncomingDispatcher incomingDispatcher;

    public void UpdateDevices(DeviceID id) {
        List<Module> components = slaveController.getModules();
        Message message = new Message(new ModulesPayload(components));
        message.putHeader(Message.HEADER_REPLY_TO_KEY, CoreConstants.RoutingKeys.APP_MODULES_GET);
        message.putHeader(Message.HEADER_TIMESTAMP, System.currentTimeMillis());
        outgoing.sendMessage(id, CoreConstants.RoutingKeys.APP_MODULES_GET, message);
    }

    @Override
    public void handle(Message.AddressedMessage message) {
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
        incomingDispatcher = dispatcher;
        container = dispatcher.getContainer();
        outgoing = container.require(OutgoingRouter.KEY);
        slaveController = container.require(SlaveController.KEY);
    }

    @Override
    public void handlerRemoved(String routingKey) {
    }
}
