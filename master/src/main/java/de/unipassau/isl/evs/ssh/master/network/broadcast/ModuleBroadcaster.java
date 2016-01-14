package de.unipassau.isl.evs.ssh.master.network.broadcast;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.network.Server;

/**
 * The ModuleBroadcaster class sends push messages to connected clients to update their information about connected
 * modules.
 *
 * @author Wolfgang Popp.
 */
public class ModuleBroadcaster extends AbstractComponent {
    public static final Key<ModuleBroadcaster> KEY = new Key<>(ModuleBroadcaster.class);

    private Message createUpdateMessage() {
        final SlaveController slaveController = requireComponent(SlaveController.KEY);
        final List<Slave> slaves = slaveController.getSlaves();
        final ListMultimap<Slave, Module> modulesAtSlave = ArrayListMultimap.create();

        for (Slave slave : slaves) {
            modulesAtSlave.putAll(slave, slaveController.getModulesOfSlave(slave.getSlaveID()));
        }

        return new Message(new ModulesPayload(modulesAtSlave, slaves));
    }

    /**
     * Sends a message with a ModulePayload to each connected client.
     */
    public void updateAllClients() {
        final Iterable<DeviceID> connectedClients = requireComponent(Server.KEY).getActiveDevices();
        for (DeviceID connectedClient : connectedClients) {
            updateClient(connectedClient);
        }
    }

    /**
     * Sends a message with a ModulePayload to the given client.
     *
     * @param id the id of the client that will receive the message
     */
    public void updateClient(DeviceID id) {
        final Message message = createUpdateMessage();
        requireComponent(OutgoingRouter.KEY).sendMessage(id, RoutingKeys.GLOBAL_MODULES_UPDATE, message);
    }
}
