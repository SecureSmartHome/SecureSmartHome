package de.unipassau.isl.evs.ssh.app.handler;

import android.util.Log;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.RegisterSlavePayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * The AppAddSlaveHandler handles the messaging needed to add a new slave to the system.
 *
 * @author Wolfgang Popp
 */
public class AppAddSlaveHandler extends AbstractComponent implements MessageHandler {
    public static final Key<AppAddSlaveHandler> KEY = new Key<>(AppAddSlaveHandler.class);
    private static final String TAG = AppAddSlaveHandler.class.getSimpleName();

    @Override
    public void handle(Message.AddressedMessage message) {
        //TODO Error Handling
        Log.e(TAG, "Received message: " + message);
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {

    }

    @Override
    public void init(Container container) {
        super.init(container);
        container.require(IncomingDispatcher.KEY).registerHandler(this, CoreConstants.RoutingKeys.APP_SLAVE_REGISTER);
    }

    @Override
    public void destroy() {
        getComponent(IncomingDispatcher.KEY).unregisterHandler(this, CoreConstants.RoutingKeys.APP_SLAVE_REGISTER);
        super.destroy();
    }

    /**
     * Sends a message to master to registers a new slave.
     * @param slaveID the device ID of the new slave
     * @param slaveName the name of the new slave
     */
    public void registerNewSlave(DeviceID slaveID, String slaveName) {
        Log.v(TAG, "registerNewSlave() called");
        RegisterSlavePayload payload = new RegisterSlavePayload(slaveName, slaveID);
        Message message = new Message(payload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, CoreConstants.RoutingKeys.APP_SLAVE_REGISTER);
        getComponent(OutgoingRouter.KEY).sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_SLAVE_REGISTER, message);
    }
}
