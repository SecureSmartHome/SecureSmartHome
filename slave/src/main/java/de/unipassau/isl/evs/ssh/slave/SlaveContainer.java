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

package de.unipassau.isl.evs.ssh.slave;

import android.util.Log;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AccessLogger;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.network.ClientOutgoingRouter;
import de.unipassau.isl.evs.ssh.core.network.UDPDiscoveryClient;
import de.unipassau.isl.evs.ssh.core.schedule.DefaultExecutionServiceComponent;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import de.unipassau.isl.evs.ssh.slave.handler.SlaveCameraHandler;
import de.unipassau.isl.evs.ssh.slave.handler.SlaveDoorHandler;
import de.unipassau.isl.evs.ssh.slave.handler.SlaveLightHandler;
import de.unipassau.isl.evs.ssh.slave.handler.SlaveModuleHandler;
import de.unipassau.isl.evs.ssh.slave.handler.SlaveSystemHealthChecker;

/**
 * This Container class manages dependencies needed in the Slave part of the architecture.
 *
 * @author Team
 */
public class SlaveContainer extends ContainerService {
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

        register(SlaveModuleHandler.KEY, new SlaveModuleHandler());
        register(SlaveSystemHealthChecker.KEY, new SlaveSystemHealthChecker());

        registerHandler(new SlaveLightHandler());
        registerHandler(new SlaveDoorHandler());
        registerHandler(new SlaveCameraHandler());

        final NamingManager namingManager = require(NamingManager.KEY);
        Log.i(getClass().getSimpleName(), "Slave set up! ID is " + namingManager.getOwnID()
                + "; Master is " + (namingManager.isMasterKnown() ? namingManager.getMasterID() : "unknown"));
        Log.i(getClass().getSimpleName(), "Routing Table set in " + require(IncomingDispatcher.KEY).toString());
    }

    private void registerHandler(AbstractMessageHandler messageHandler) {
        require(IncomingDispatcher.KEY).registerHandler(messageHandler, messageHandler.getRoutingKeys());
    }
}