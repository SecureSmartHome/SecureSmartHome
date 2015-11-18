package de.unipassau.isl.evs.ssh.master;

import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.master.database.DatabaseConnector;

/**
 * This Container class manages dependencies needed in the Master part of the architecture.
 */
public class MasterContainer extends ContainerService {

    public MasterContainer() {
    }

    @Override
    protected void init() {
        register(DatabaseConnector.KEY, new DatabaseConnector());
    }
}