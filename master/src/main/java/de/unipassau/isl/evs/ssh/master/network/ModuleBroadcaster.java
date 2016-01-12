package de.unipassau.isl.evs.ssh.master.network;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.network.Server;

/**
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

    public void updateAllClients() {
        final Iterable<DeviceID> connectedClients = requireComponent(Server.KEY).getActiveDevices();
        for (DeviceID connectedClient : connectedClients) {
            updateClient(connectedClient);
        }
    }

    public void updateClient(DeviceID id) {
        final Message message = createUpdateMessage();
        requireComponent(OutgoingRouter.KEY).sendMessage(id, RoutingKeys.GLOBAL_MODULES_UPDATE, message);
    }
}
