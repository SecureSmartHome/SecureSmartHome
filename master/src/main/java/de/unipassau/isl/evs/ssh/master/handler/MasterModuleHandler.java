package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Log;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
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

    public void updateDevices(DeviceID id) {
        SlaveController slaveController = getComponent(SlaveController.KEY);
        OutgoingRouter router = getComponent(OutgoingRouter.KEY);

        List<Module> components = slaveController.getModules();
        List<Slave> slaves = slaveController.getSlaves();
        Message message = new Message(new ModulesPayload(components, slaves));
        message.putHeader(Message.HEADER_REPLY_TO_KEY, CoreConstants.RoutingKeys.APP_MODULES_GET);
        message.putHeader(Message.HEADER_TIMESTAMP, System.currentTimeMillis());
        router.sendMessage(id, CoreConstants.RoutingKeys.APP_MODULES_GET, message);
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        // TODO handle add new sensor
        // TODO handle remove sensor
        // TODO handle get modules
    }

    private void handleAddModule(Module module, Message.AddressedMessage message) {
        SlaveController controller = getComponent(SlaveController.KEY);
        //TODO create permission for the new module
        try {
            controller.addModule(module);
        } catch (DatabaseControllerException e) {
            Log.e(TAG, "Error while adding new module: " + e.getCause().getMessage());
            sendErrorMessage(message);
        }
    }

    private void handleRemoveModule(String moduleName, Message.AddressedMessage message) {
        SlaveController controller = getComponent(SlaveController.KEY);
        controller.removeModule(moduleName);
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {
    }
}
