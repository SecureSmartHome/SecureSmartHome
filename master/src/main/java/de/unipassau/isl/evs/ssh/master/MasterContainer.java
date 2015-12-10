package de.unipassau.isl.evs.ssh.master;

import android.util.Log;

import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import de.unipassau.isl.evs.ssh.master.database.DatabaseConnector;
import de.unipassau.isl.evs.ssh.master.database.HolidayController;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;
import de.unipassau.isl.evs.ssh.master.handler.MasterClimateHandler;
import de.unipassau.isl.evs.ssh.master.handler.MasterLightHandler;
import de.unipassau.isl.evs.ssh.master.handler.MasterModuleHandler;
import de.unipassau.isl.evs.ssh.master.handler.MasterNotificationHandler;
import de.unipassau.isl.evs.ssh.master.handler.MasterRegisterDeviceHandler;
import de.unipassau.isl.evs.ssh.master.handler.MasterRoutingTableHandler;
import de.unipassau.isl.evs.ssh.master.handler.MasterUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.master.network.Server;
import de.unipassau.isl.evs.ssh.master.task.MasterHolidaySimulationPlannerHandler;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_DEVICE_CONNECTED;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_HOLIDAY_GET;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_LIGHT_GET;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_LIGHT_SET;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_MODULE_ADD;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_PUSH_WEATHER_INFO;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_REQUEST_WEATHER_INFO;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_SLAVE_REGISTER;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_USERINFO_GET;

/**
 * This Container class manages dependencies needed in the Master part of the architecture.
 *
 * @author Team
 */
public class MasterContainer extends ContainerService {
    @Override
    protected void init() {
        register(DatabaseConnector.KEY, new DatabaseConnector());
        register(KeyStoreController.KEY, new KeyStoreController());
        register(NamingManager.KEY, new NamingManager(true));
        register(Server.KEY, new Server());
        register(SlaveController.KEY, new SlaveController());
        register(PermissionController.KEY, new PermissionController());
        register(HolidayController.KEY, new HolidayController());
        register(UserManagementController.KEY, new UserManagementController());
        register(MasterRegisterDeviceHandler.KEY, new MasterRegisterDeviceHandler());

        final IncomingDispatcher incomingDispatcher = require(IncomingDispatcher.KEY);
        incomingDispatcher.registerHandler(new MasterLightHandler(), MASTER_LIGHT_SET, MASTER_LIGHT_GET);
        incomingDispatcher.registerHandler(new MasterClimateHandler(), MASTER_LIGHT_GET, MASTER_REQUEST_WEATHER_INFO, MASTER_PUSH_WEATHER_INFO);
        incomingDispatcher.registerHandler(new MasterNotificationHandler(), MASTER_NOTIFICATION_SEND);
        incomingDispatcher.registerHandler(new MasterUserConfigurationHandler(), MASTER_USERINFO_GET, MASTER_DEVICE_CONNECTED);
        incomingDispatcher.registerHandler(new MasterModuleHandler(), MASTER_MODULE_ADD);
        incomingDispatcher.registerHandler(new MasterHolidaySimulationPlannerHandler(), MASTER_HOLIDAY_GET);
        incomingDispatcher.registerHandler(new MasterRoutingTableHandler(), MASTER_SLAVE_REGISTER);

        Log.i(getClass().getSimpleName(), "Master set up! ID is " + require(NamingManager.KEY).getOwnID());
    }
}