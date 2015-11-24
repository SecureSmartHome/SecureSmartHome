package de.unipassau.isl.evs.ssh.slave;

import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;

/**
 * This Container class manages dependencies needed in the Slave part of the architecture.
 */
public class SlaveContainer extends ContainerService {
    @Override
    protected void init() {
        getSharedPreferences(SlaveConstants.FILE_SHARED_PREFS, MODE_PRIVATE).edit()
                .putString(SlaveConstants.PREF_HOST, "localhost")
                .commit();

        register(KeyStoreController.KEY, new KeyStoreController());
        register(NamingManager.KEY, new NamingManager(true));
        register(Client.KEY, new Client());
    }
}