package de.unipassau.isl.evs.ssh.master;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
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
import de.unipassau.isl.evs.ssh.master.handler.MasterUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.master.network.Server;
import de.unipassau.isl.evs.ssh.master.task.MasterHolidaySimulationPlannerHandler;
import de.unipassau.isl.evs.ssh.master.task.MasterWeatherCheckHandler;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.*;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_DEVICE_CONNECTED;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_LIGHT_GET;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_LIGHT_SET;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_MODULE_ADD;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_REGISTER_FINALIZE;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_REGISTER_INIT;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_USERINFO_GET;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_WEATHER_INFO;

/**
 * This Container class manages dependencies needed in the Master part of the architecture.
 *
 * @author Niko
 */
public class MasterContainer extends ContainerService {
    private static File dir = new File("/sdcard/ssh");

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

        final IncomingDispatcher incomingDispatcher = require(IncomingDispatcher.KEY);

        incomingDispatcher.registerHandler(new MasterLightHandler(), CoreConstants.RoutingKeys.MASTER_LIGHT_SET,
                CoreConstants.RoutingKeys.MASTER_LIGHT_GET);
        incomingDispatcher.registerHandler(new MasterClimateHandler(), CoreConstants.RoutingKeys.MASTER_LIGHT_GET,
                CoreConstants.RoutingKeys.MASTER_WEATHER_INFO);
        incomingDispatcher.registerHandler(new MasterNotificationHandler(),
                CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND);
        incomingDispatcher.registerHandler(new MasterUserConfigurationHandler(), CoreConstants.RoutingKeys.MASTER_USERINFO_GET);
        incomingDispatcher.registerHandler(new MasterModuleHandler(), CoreConstants.RoutingKeys.MASTER_MODULE_ADD);
        incomingDispatcher.registerHandler(new MasterRegisterDeviceHandler(),
                CoreConstants.RoutingKeys.MASTER_REGISTER_INIT, CoreConstants.RoutingKeys.MASTER_REGISTER_FINALIZE);
        incomingDispatcher.registerHandler(new MasterHolidaySimulationPlannerHandler(),
                CoreConstants.RoutingKeys.MASTER_HOLIDAY_GET, CoreConstants.RoutingKeys.MASTER_HOLIDAY_GET);
        incomingDispatcher.registerHandler(new MasterWeatherCheckHandler(),
                CoreConstants.RoutingKeys.MASTER_DOOR_STATUS_GET);

        if (!dir.isDirectory() && !dir.mkdirs()) {
            dir = getFilesDir();
        }
        Log.i("ContainerService", "Storing IDs in " + dir);

        Log.i(getClass().getSimpleName(), "Master set up! ID is " + require(NamingManager.KEY).getOwnID());

        // write the master id and cert to local storage so that it can be copied to slaves as long as
        // adding new devices is not implemented
        writeMasterId();
        writeMasterCert();
    }

    private void writeMasterId() {
        try (final FileOutputStream os = new FileOutputStream(new File(dir, "master.id"))) {
            os.write(require(NamingManager.KEY).getOwnID().getIDString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeMasterCert() {
        try (final FileOutputStream os = new FileOutputStream(new File(dir, "master.der"))) {
            os.write(require(NamingManager.KEY).getOwnCertificate().getEncoded());
        } catch (IOException | CertificateEncodingException e) {
            e.printStackTrace();
        }
    }
}