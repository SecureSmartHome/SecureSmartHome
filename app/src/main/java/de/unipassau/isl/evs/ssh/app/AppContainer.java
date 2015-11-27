package de.unipassau.isl.evs.ssh.app;

import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.network.Client;

/**
 * This Container class manages dependencies needed in the Android App.
 */
public class AppContainer extends ContainerService {
    @Override
    protected void init() {
        register(Client.KEY, new Client());
        register(AppModuleHandler.KEY, new AppModuleHandler());
    }
}