package de.unipassau.isl.evs.ssh.app;

import android.util.Log;

import de.unipassau.isl.evs.ssh.app.handler.AppClimateHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppDoorHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppHolidaySimulationHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppLightHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppModifyModuleHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppModuleHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppNotificationHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppRegisterNewDeviceHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppSlaveManagementHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AccessLogger;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.network.ClientOutgoingRouter;
import de.unipassau.isl.evs.ssh.core.network.UDPDiscoveryClient;
import de.unipassau.isl.evs.ssh.core.schedule.DefaultExecutionServiceComponent;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
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
        register(ExecutionServiceComponent.KEY, new DefaultExecutionServiceComponent(getClass().getSimpleName()));
        if (CoreConstants.TRACK_STATISTICS) {
            register(AccessLogger.KEY, new AccessLogger());
        }

        register(IncomingDispatcher.KEY, new IncomingDispatcher());
        register(OutgoingRouter.KEY, new ClientOutgoingRouter());
        register(UDPDiscoveryClient.KEY, new UDPDiscoveryClient());
        register(Client.KEY, new Client());

        register(AppModuleHandler.KEY, new AppModuleHandler());

        register(AppSlaveManagementHandler.KEY, new AppSlaveManagementHandler());
        register(AppClimateHandler.KEY, new AppClimateHandler());
        register(AppDoorHandler.KEY, new AppDoorHandler());
        register(AppHolidaySimulationHandler.KEY, new AppHolidaySimulationHandler());
        register(AppLightHandler.KEY, new AppLightHandler());
        register(AppModifyModuleHandler.KEY, new AppModifyModuleHandler());
        register(AppNotificationHandler.KEY, new AppNotificationHandler());
        register(AppRegisterNewDeviceHandler.KEY, new AppRegisterNewDeviceHandler());
        register(AppUserConfigurationHandler.KEY, new AppUserConfigurationHandler());

        Log.i(getClass().getSimpleName(), "Routing Table set in " + require(IncomingDispatcher.KEY).toString());
    }
}