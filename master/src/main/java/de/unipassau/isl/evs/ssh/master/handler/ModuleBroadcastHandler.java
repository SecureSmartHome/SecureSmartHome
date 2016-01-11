package de.unipassau.isl.evs.ssh.master.handler;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.network.Server;

/**
 * @author Wolfgang Popp.
 */
public abstract class ModuleBroadcastHandler extends AbstractMasterHandler {


    @Override
    public abstract RoutingKey[] getRoutingKeys();

    @Override
    public abstract void handle(Message.AddressedMessage message);


    private Message createUpdateMessage() {
        final SlaveController slaveController = requireComponent(SlaveController.KEY);
        final List<Slave> slaves = slaveController.getSlaves();
        final ListMultimap<Slave, Module> modulesAtSlave = ArrayListMultimap.create();

        for (Slave slave : slaves) {
            modulesAtSlave.putAll(slave, slaveController.getModulesOfSlave(slave.getSlaveID()));
        }

        return new Message(new ModulesPayload(modulesAtSlave, slaves));
    }

    protected void updateAllClients() {
        final Iterable<DeviceID> connectedClients = requireComponent(Server.KEY).getActiveDevices();
        for (DeviceID connectedClient : connectedClients) {
            updateClient(connectedClient);
        }
    }

    protected void updateClient(DeviceID id) {
        final Message message = createUpdateMessage();
        sendMessage(id, RoutingKeys.GLOBAL_MODULES_UPDATE, message);
    }
}
