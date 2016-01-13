package de.unipassau.isl.evs.ssh.master;

import android.util.Log;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AccessLogger;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.schedule.DefaultExecutionServiceComponent;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import de.unipassau.isl.evs.ssh.master.database.DatabaseConnector;
import de.unipassau.isl.evs.ssh.master.database.HolidayController;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;
import de.unipassau.isl.evs.ssh.master.handler.AbstractMasterHandler;
import de.unipassau.isl.evs.ssh.master.handler.MasterCameraHandler;
import de.unipassau.isl.evs.ssh.master.handler.MasterClimateHandler;
import de.unipassau.isl.evs.ssh.master.handler.MasterDoorBellHandler;
import de.unipassau.isl.evs.ssh.master.handler.MasterDoorHandler;
import de.unipassau.isl.evs.ssh.master.handler.MasterLightHandler;
import de.unipassau.isl.evs.ssh.master.handler.MasterModuleHandler;
import de.unipassau.isl.evs.ssh.master.handler.MasterRegisterDeviceHandler;
import de.unipassau.isl.evs.ssh.master.handler.MasterSlaveManagementHandler;
import de.unipassau.isl.evs.ssh.master.handler.MasterSystemHealthCheckHandler;
import de.unipassau.isl.evs.ssh.master.handler.MasterUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.master.handler.MasterUserLocationHandler;
import de.unipassau.isl.evs.ssh.master.network.ModuleBroadcaster;
import de.unipassau.isl.evs.ssh.master.network.NotificationBroadcaster;
import de.unipassau.isl.evs.ssh.master.network.Server;
import de.unipassau.isl.evs.ssh.master.network.ServerOutgoingRouter;
import de.unipassau.isl.evs.ssh.master.network.UDPDiscoveryServer;
import de.unipassau.isl.evs.ssh.master.network.UserConfigurationBroadcaster;
import de.unipassau.isl.evs.ssh.master.task.MasterHolidaySimulationPlannerHandler;
import de.unipassau.isl.evs.ssh.master.task.MasterWeatherCheckHandler;

/**
 * This Container class manages dependencies needed in the Master part of the architecture.
 * TODO Niko JavaDoc in Module
 *
 * @author Team
 */
public class MasterContainer extends ContainerService {
    @Override
    protected void init() {
        register(DatabaseConnector.KEY, new DatabaseConnector());
        register(KeyStoreController.KEY, new KeyStoreController());
        register(NamingManager.KEY, new NamingManager(true));
        register(ExecutionServiceComponent.KEY, new DefaultExecutionServiceComponent(getClass().getSimpleName()));
        if (CoreConstants.TRACK_STATISTICS) {
            register(AccessLogger.KEY, new AccessLogger());
        }

        register(IncomingDispatcher.KEY, new IncomingDispatcher());
        register(OutgoingRouter.KEY, new ServerOutgoingRouter());
        register(UDPDiscoveryServer.KEY, new UDPDiscoveryServer());
        register(Server.KEY, new Server());

        register(SlaveController.KEY, new SlaveController());
        register(PermissionController.KEY, new PermissionController());
        register(HolidayController.KEY, new HolidayController());
        register(UserManagementController.KEY, new UserManagementController());

        register(MasterRegisterDeviceHandler.KEY, new MasterRegisterDeviceHandler());
        register(MasterUserLocationHandler.KEY, new MasterUserLocationHandler());
        register(MasterClimateHandler.KEY, new MasterClimateHandler());
        register(MasterSlaveManagementHandler.KEY, new MasterSlaveManagementHandler());

        register(NotificationBroadcaster.KEY, new NotificationBroadcaster());
        register(ModuleBroadcaster.KEY, new ModuleBroadcaster());
        register(UserConfigurationBroadcaster.KEY, new UserConfigurationBroadcaster());

        register(MasterWeatherCheckHandler.KEY, new MasterWeatherCheckHandler());
        register(MasterHolidaySimulationPlannerHandler.KEY, new MasterHolidaySimulationPlannerHandler());


        registerHandler(new MasterDoorBellHandler());
        registerHandler(new MasterModuleHandler());
        registerHandler(new MasterUserConfigurationHandler());
        registerHandler(new MasterLightHandler());
        registerHandler(new MasterSystemHealthCheckHandler());
        registerHandler(new MasterCameraHandler());


        registerHandler(new MasterDoorHandler());
        //registerHandler(new MasterPermissionHandler());

        Log.i(getClass().getSimpleName(), "Master set up! ID is " + require(NamingManager.KEY).getOwnID());
        Log.i(getClass().getSimpleName(), "Routing Table set in " + require(IncomingDispatcher.KEY).toString());
    }

    private void registerHandler(AbstractMasterHandler masterHandler) {
        require(IncomingDispatcher.KEY).registerHandler(masterHandler, masterHandler.getRoutingKeys());
    }
}