package de.unipassau.isl.evs.ssh.app;

import de.unipassau.isl.evs.ssh.app.handler.AppAddSlaveHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppClimateHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppDoorHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppHolidaySimulationHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppLightHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppModuleHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppNewModuleHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppNotificationHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppRegisterNewDeviceHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;

/**
 * This Container class manages dependencies needed in the Android App.
 *
 * @author Team
 */
public class AppContainer extends ContainerService {
    @Override
    protected void init() {
        register(KeyStoreController.KEY, new KeyStoreController());
        register(NamingManager.KEY, new NamingManager(false));
        register(Client.KEY, new Client());
        register(AppModuleHandler.KEY, new AppModuleHandler());
        register(AppDoorHandler.KEY, new AppDoorHandler());
        register(AppLightHandler.KEY, new AppLightHandler());
        register(AppClimateHandler.KEY, new AppClimateHandler());
        register(AppNotificationHandler.KEY, new AppNotificationHandler());
        register(AppUserConfigurationHandler.KEY, new AppUserConfigurationHandler());
        register(AppNewModuleHandler.KEY, new AppNewModuleHandler());
        register(AppRegisterNewDeviceHandler.KEY, new AppRegisterNewDeviceHandler());
        register(AppHolidaySimulationHandler.KEY, new AppHolidaySimulationHandler());
        register(AppAddSlaveHandler.KEY, new AppAddSlaveHandler());

    }
}