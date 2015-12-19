package de.unipassau.isl.evs.ssh.app.handler;

import android.util.Log;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.RegisterSlavePayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.APP_SLAVE_REGISTER;

/**
 * The AppAddSlaveHandler handles the messaging needed to add a new slave to the system.
 *
 * @author Wolfgang Popp
 */
public class AppAddSlaveHandler extends AbstractMessageHandler implements Component {
    public static final Key<AppAddSlaveHandler> KEY = new Key<>(AppAddSlaveHandler.class);
    private static final String TAG = AppAddSlaveHandler.class.getSimpleName();

    @Override
    public void handle(Message.AddressedMessage message) {
        //TODO Error Handling
        Log.e(TAG, "Received message: " + message);
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{APP_SLAVE_REGISTER};
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
        message.putHeader(Message.HEADER_REPLY_TO_KEY, APP_SLAVE_REGISTER.getKey());
        getComponent(OutgoingRouter.KEY).sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_SLAVE_REGISTER, message);
    }
}
