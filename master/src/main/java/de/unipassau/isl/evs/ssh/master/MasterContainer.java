/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
import de.unipassau.isl.evs.ssh.master.network.Server;
import de.unipassau.isl.evs.ssh.master.network.ServerOutgoingRouter;
import de.unipassau.isl.evs.ssh.master.network.UDPDiscoveryServer;
import de.unipassau.isl.evs.ssh.master.network.broadcast.ModuleBroadcaster;
import de.unipassau.isl.evs.ssh.master.network.broadcast.NotificationBroadcaster;
import de.unipassau.isl.evs.ssh.master.network.broadcast.UserConfigurationBroadcaster;
import de.unipassau.isl.evs.ssh.master.task.MasterHolidaySimulationPlannerHandler;
import de.unipassau.isl.evs.ssh.master.task.MasterWeatherCheckHandler;

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