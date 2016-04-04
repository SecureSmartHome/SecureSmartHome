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