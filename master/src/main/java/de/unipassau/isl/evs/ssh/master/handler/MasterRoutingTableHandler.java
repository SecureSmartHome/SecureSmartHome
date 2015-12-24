package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Log;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.RegisterSlavePayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.AlreadyInUseException;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.network.Server;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.GLOBAL_MODULES_UPDATE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_SLAVE_REGISTER;

/**
 * Handles messages indicating that information of a device needs to be updated and writes these changes to the routing table.
 * <p/>
 * An example when this handler needs to take action is when a new sensor is added or switched to a new GPIO Pin.
 *
 * @author Leon Sell
 */
public class MasterRoutingTableHandler extends AbstractMasterHandler implements Component {
    public static final Key<MasterRoutingTableHandler> KEY = new Key<>(MasterRoutingTableHandler.class);
    private static final String TAG = MasterRoutingTableHandler.class.getSimpleName();

    @Override
    public void handle(Message.AddressedMessage message) {
        saveMessage(message);
        if (MASTER_SLAVE_REGISTER.matches(message)) {
            if (hasPermission(message.getFromID(), new Permission(de.unipassau.isl.evs.ssh.core.sec.Permission.ADD_ODROID.toString()))) {
                RegisterSlavePayload registerSlavePayload = MASTER_SLAVE_REGISTER.getPayload(message);
                try {
                    registerSlave(new Slave(
                            registerSlavePayload.getName(),
                            registerSlavePayload.getSlaveID(),
                            registerSlavePayload.getPassiveRegistrationToken()
                    ));
                } catch (AlreadyInUseException e) {
                    Log.i(TAG, "Failed adding a new Slave because the given name (" + registerSlavePayload.getName()
                            + ") is already in use.");
                    sendErrorMessage(message);
                }
            } else {
                //TODO handle (Niko, 2015-12-17)
            }
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            handleErrorMessage(message);
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_SLAVE_REGISTER};
    }

    public void registerSlave(Slave slave) throws AlreadyInUseException {
        requireComponent(SlaveController.KEY).addSlave(slave);
        updateAllClients();
    }

    private void updateAllClients() {
        Iterable<DeviceID> connectedClients = requireComponent(Server.KEY).getActiveDevices();
        for (DeviceID connectedClient : connectedClients) {
            updateClient(connectedClient);
        }
    }

    public void updateClient(DeviceID id) {
        OutgoingRouter router = getComponent(OutgoingRouter.KEY);
        Message message = createUpdateMessage();
        router.sendMessage(id, GLOBAL_MODULES_UPDATE, message);
    }

    private Message createUpdateMessage() {
        SlaveController slaveController = getComponent(SlaveController.KEY);
        List<Slave> slaves = slaveController.getSlaves();
        ListMultimap<Slave, Module> modulesAtSlave = ArrayListMultimap.create();

        for (Slave slave : slaves) {
            modulesAtSlave.putAll(slave, slaveController.getModulesOfSlave(slave.getSlaveID()));
        }

        return new Message(new ModulesPayload(modulesAtSlave, slaves));
    }
}