package de.unipassau.isl.evs.ssh.slave;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import de.unipassau.isl.evs.ssh.drivers.lib.EdimaxPlugSwitch;
import de.unipassau.isl.evs.ssh.slave.handler.SlaveDoorHandler;
import de.unipassau.isl.evs.ssh.slave.handler.SlaveLightHandler;
import de.unipassau.isl.evs.ssh.slave.handler.SlaveModuleHandler;

/**
 * This Container class manages dependencies needed in the Slave part of the architecture.
 *
 * @author Niko
 */
public class SlaveContainer extends ContainerService {
    private static final File dir = new File("/sdcard/ssh");

    @Override
    protected void init() {
        register(KeyStoreController.KEY, new KeyStoreController());

        // read the master id and cert from local storage as long as adding new devices is not implemented
        readMasterData();

        register(NamingManager.KEY, new NamingManager(false));

        // write the app id and cert to local storage as long as adding new devices is not implemented
        dir.mkdirs();
        writeSlaveId();
        writeSlaveCert();

        register(Client.KEY, new Client());
        register(SlaveModuleHandler.KEY, new SlaveModuleHandler());

        final IncomingDispatcher incomingDispatcher = require(IncomingDispatcher.KEY);
        incomingDispatcher.registerHandler(new SlaveLightHandler(),
                CoreConstants.RoutingKeys.SLAVE_LIGHT_GET, CoreConstants.RoutingKeys.SLAVE_LIGHT_SET);

        incomingDispatcher.registerHandler(new SlaveDoorHandler(),
                CoreConstants.RoutingKeys.SLAVE_DOOR_STATUS_GET,
                CoreConstants.RoutingKeys.SLAVE_DOOR_UNLATCH);

        //FIXME this is temporary for testing until we got everything needed
        Key<EdimaxPlugSwitch> key = new Key<>(EdimaxPlugSwitch.class, "TestPlugswitch");
        register(key, new EdimaxPlugSwitch("192.168.0.111", 10000, "admin", "1234"));

        final NamingManager namingManager = require(NamingManager.KEY);
        Log.i(getClass().getSimpleName(), "Slave set up! ID is " + namingManager.getOwnID()
                + "; Master is " + namingManager.getMasterID());
    }

    private void readMasterData() {
        final File masterCert = new File(dir, "master.der");
        if (masterCert.exists()) {
            try {
                CertificateFactory certFact = CertificateFactory.getInstance("X.509");
                final Certificate certificate = certFact.generateCertificate(new FileInputStream(masterCert));

                final DeviceID id = DeviceID.fromCertificate((X509Certificate) certificate);
                getSharedPreferences(CoreConstants.FILE_SHARED_PREFS, MODE_PRIVATE).edit()
                        .putString(SlaveConstants.SharedPrefs.PREF_MASTER_ID, id.getIDString())
                        .commit();

                require(KeyStoreController.KEY).saveCertificate(((X509Certificate) certificate), id.getIDString());
                Log.i(getClass().getSimpleName(), "Certificate for Master " + id + " loaded:\n" + certificate);
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeSlaveCert() {
        try (final FileOutputStream os = new FileOutputStream(new File(dir, "slave.der"))) {
            os.write(require(KeyStoreController.KEY).getOwnCertificate().getEncoded());
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    private void writeSlaveId() {
        try (final FileOutputStream os = new FileOutputStream(new File(dir, "slave.id"))) {
            os.write(require(NamingManager.KEY).getOwnID().getId().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}