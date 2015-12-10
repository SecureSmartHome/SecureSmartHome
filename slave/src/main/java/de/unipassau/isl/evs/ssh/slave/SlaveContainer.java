package de.unipassau.isl.evs.ssh.slave;

import android.util.Log;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import de.unipassau.isl.evs.ssh.slave.handler.SlaveDoorHandler;
import de.unipassau.isl.evs.ssh.slave.handler.SlaveLightHandler;
import de.unipassau.isl.evs.ssh.slave.handler.SlaveModuleHandler;
import de.unipassau.isl.evs.ssh.slave.handler.SlaveSystemHealthHandler;

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
        register(Client.KEY, new Client());
        register(SlaveModuleHandler.KEY, new SlaveModuleHandler());
        register(ExecutionServiceComponent.KEY, new ExecutionServiceComponent());
        register(SlaveSystemHealthHandler.KEY, new SlaveSystemHealthHandler());

        final IncomingDispatcher incomingDispatcher = require(IncomingDispatcher.KEY);
        incomingDispatcher.registerHandler(new SlaveLightHandler(),
                CoreConstants.RoutingKeys.SLAVE_LIGHT_GET, CoreConstants.RoutingKeys.SLAVE_LIGHT_SET);

        incomingDispatcher.registerHandler(new SlaveDoorHandler(),
                CoreConstants.RoutingKeys.SLAVE_DOOR_STATUS_GET,
                CoreConstants.RoutingKeys.SLAVE_DOOR_UNLATCH);

        //FIXME this is temporary for testing until we got everything needed
        //Key<EdimaxPlugSwitch> key = new Key<>(EdimaxPlugSwitch.class, "TestPlugswitch");
        //register(key, new EdimaxPlugSwitch("192.168.0.111", 10000, "admin", "1234"));

        final NamingManager namingManager = require(NamingManager.KEY);
        Log.i(getClass().getSimpleName(), "Slave set up! ID is " + namingManager.getOwnID()
                + "; Master is " + (namingManager.isMasterKnown() ? namingManager.getMasterID() : "unknown"));
    }
}