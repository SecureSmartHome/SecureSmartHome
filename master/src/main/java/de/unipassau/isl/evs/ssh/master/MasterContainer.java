package de.unipassau.isl.evs.ssh.master;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import de.unipassau.isl.evs.ssh.master.database.DatabaseConnector;
import de.unipassau.isl.evs.ssh.master.network.Server;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * This Container class manages dependencies needed in the Master part of the architecture.
 *
 * @author Niko
 */
public class MasterContainer extends ContainerService {
    private static final File dir = new File("/sdcard/ssh");

    @Override
    protected void init() {
        register(DatabaseConnector.KEY, new DatabaseConnector());
        register(KeyStoreController.KEY, new KeyStoreController());
        register(NamingManager.KEY, new NamingManager(true));
        register(Server.KEY, new Server());

        dir.mkdirs();

        Log.i(getClass().getSimpleName(), "Master set up! ID is " + require(NamingManager.KEY).getLocalDeviceId());

        // write the master id and cert to local storage so that it can be copied to slaves as long as
        // adding new devices is not implemented
        writeMasterId();
        writeMasterCert();
    }

    private void writeMasterId() {
        try (final FileOutputStream os = new FileOutputStream(new File(dir, "master.id"))) {
            os.write(require(NamingManager.KEY).getLocalDeviceId().getId().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeMasterCert() {
        try (final FileOutputStream os = new FileOutputStream(new File(dir, "master.der"))) {
            os.write(require(KeyStoreController.KEY).getOwnCertificate().getEncoded());
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }
}