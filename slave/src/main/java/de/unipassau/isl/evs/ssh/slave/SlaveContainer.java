package de.unipassau.isl.evs.ssh.slave;

import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

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
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import de.unipassau.isl.evs.ssh.drivers.lib.EdimaxPlugSwitch;
import de.unipassau.isl.evs.ssh.slave.handler.SlaveLightHandler;

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
        register(NamingManager.KEY, new NamingManager(false));
        register(Client.KEY, new Client());

        final IncomingDispatcher incomingDispatcher = require(IncomingDispatcher.KEY);
        incomingDispatcher.registerHandler(new SlaveLightHandler(),
                CoreConstants.RoutingKeys.SLAVE_LIGHT_GET, CoreConstants.RoutingKeys.SLAVE_LIGHT_SET);

        //FIXME this is temporary for testing until we got everything needed
        Key<EdimaxPlugSwitch> key = new Key<>(EdimaxPlugSwitch.class, "TestPlugswitch");
        register(key, new EdimaxPlugSwitch("192.168.0.111", 10000, "admin", "1234"));
        syncKeyStore();

        final NamingManager namingManager = require(NamingManager.KEY);
        Log.i(getClass().getSimpleName(), "Slave set up! ID is " + namingManager.getLocalDeviceId()
                + "; Master is " + namingManager.getMasterID());
    }

    private void syncKeyStore() {
        dir.mkdirs();

        // read the master id and cert from local storage as long as adding new devices is not implemented
        readMasterId();
        readMasterCert();

        // write the slave id and cert to local storage as long as adding new devices is not implemented
        writeSlaveId();
        writeSlaveCert();
    }

    private void readMasterId() {
        final File masterId = new File(dir, "master.id");
        if (masterId.exists()) {
            try {
                final String id = Files.readFirstLine(masterId, Charsets.US_ASCII);
                getSharedPreferences(SlaveConstants.FILE_SHARED_PREFS, MODE_PRIVATE).edit()
                        .putString(SlaveConstants.SharedPrefs.PREF_MASTER_ID, id)
                        .commit();
                Log.i(getClass().getSimpleName(), "ID for Master loaded: " + id);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void readMasterCert() {
        final File masterCert = new File(dir, "master.der");
        if (masterCert.exists()) {
            try {
                CertificateFactory certFact = CertificateFactory.getInstance("X.509");
                final Certificate certificate = certFact.generateCertificate(new FileInputStream(masterCert));
                require(KeyStoreController.KEY).saveCertifcate(((X509Certificate) certificate),
                        require(NamingManager.KEY).getMasterID().getId());
                Log.i(getClass().getSimpleName(), "Certificate for Master loaded:\n" + certificate);
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
            os.write(require(NamingManager.KEY).getLocalDeviceId().getId().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}