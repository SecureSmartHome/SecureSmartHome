package de.unipassau.isl.evs.ssh.master;

import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import de.unipassau.isl.evs.ssh.master.database.DatabaseConnector;
import de.unipassau.isl.evs.ssh.master.network.Server;
import de.unipassau.isl.evs.ssh.master.network.ServerOutgoingRouter;

/**
 * This Container class manages dependencies needed in the Master part of the architecture.
 */
public class MasterContainer extends ContainerService {
    @Override
    protected void init() {
        register(DatabaseConnector.KEY, new DatabaseConnector());
        register(KeyStoreController.KEY, new KeyStoreController());
        register(NamingManager.KEY, new NamingManager(true));
        register(Server.KEY, new Server());
    }
}