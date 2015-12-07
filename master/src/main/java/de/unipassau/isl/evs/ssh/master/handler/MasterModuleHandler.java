package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Log;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.AddNewModulePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.DatabaseControllerException;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

/**
 * The MasterModuleHandler sends updated lists of active Modules to ODROIDs and Clients
 *
 * @author bucher
 * @author Wolfgang Popp
 */
public class MasterModuleHandler extends AbstractMasterHandler {

    private static final String TAG = MasterModuleHandler.class.getSimpleName();

    private Message createUpdateMessage() {
        SlaveController slaveController = getComponent(SlaveController.KEY);
        List<Module> components = slaveController.getModules();
        List<Slave> slaves = slaveController.getSlaves();

        Message message = new Message(new ModulesPayload(components, slaves));
        message.putHeader(Message.HEADER_TIMESTAMP, System.currentTimeMillis());

        return message;
    }

    private void updateAllClients(){
       Iterable<DeviceID> connectedClients = null;// TODO
        for (DeviceID connectedClient : connectedClients) {
           updateClient(connectedClient);
        }
    }

    public void updateClient(DeviceID id) {
        OutgoingRouter router = getComponent(OutgoingRouter.KEY);
        Message message = createUpdateMessage();
        router.sendMessage(id, CoreConstants.RoutingKeys.MODULES_UPDATE, message);
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        String routingKey = message.getRoutingKey();
        if (routingKey.equals(CoreConstants.RoutingKeys.MASTER_MODULE_ADD)) {
            if (message.getPayload() instanceof AddNewModulePayload) {
                AddNewModulePayload payload = (AddNewModulePayload) message.getPayload();
                if (handleAddModule(payload.getModule(), message)) {
                    updateAllClients();

                    Message reply = new Message(new AddNewModulePayload(null));
                    OutgoingRouter router = getComponent(OutgoingRouter.KEY);
                    router.sendMessage(message.getFromID(), message.getHeader(Message.HEADER_REPLY_TO_KEY), reply);
                }
            }
        } else if (routingKey.equals(CoreConstants.RoutingKeys.MASTER_MODULE_GET)) {
            if (message.getPayload() instanceof ModulesPayload) {
                OutgoingRouter router = getComponent(OutgoingRouter.KEY);
                router.sendMessage(message.getFromID(), message.getHeader(Message.HEADER_REPLY_TO_KEY), createUpdateMessage());
            }
        }
        // TODO handle remove sensor
    }

    private boolean handleAddModule(Module module, Message.AddressedMessage message) {
        SlaveController controller = getComponent(SlaveController.KEY);
        boolean success = false;
        //TODO create permission for the new module
        try {
            controller.addModule(module);
            success = true;
        } catch (DatabaseControllerException e) {
            Log.e(TAG, "Error while adding new module: " + e.getCause().getMessage());
            sendErrorMessage(message);
        }
        return success;
    }

    private void handleRemoveModule(String moduleName, Message.AddressedMessage message) {
        SlaveController controller = getComponent(SlaveController.KEY);
        controller.removeModule(moduleName);
        // TODO send message
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {
    }
}
