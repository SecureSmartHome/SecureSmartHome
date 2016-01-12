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
 * TODO Niko JavaDoc in Module
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