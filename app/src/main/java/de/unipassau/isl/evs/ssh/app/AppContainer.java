package de.unipassau.isl.evs.ssh.app;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import de.unipassau.isl.evs.ssh.app.handler.AppDoorHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppHolidaySimulationHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppLightHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppModuleHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppNewModuleHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppNotificationHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppRegisterNewDeviceHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;

/**
 * This Container class manages dependencies needed in the Android App.
 */
public class AppContainer extends ContainerService {
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
        register(AppModuleHandler.KEY, new AppModuleHandler());
        register(AppDoorHandler.KEY, new AppDoorHandler());
        register(AppLightHandler.KEY, new AppLightHandler());
        register(AppNotificationHandler.KEY, new AppNotificationHandler());
        register(AppUserConfigurationHandler.KEY, new AppUserConfigurationHandler());
        register(AppNewModuleHandler.KEY, new AppNewModuleHandler());
        register(AppRegisterNewDeviceHandler.KEY, new AppRegisterNewDeviceHandler());
        register(AppHolidaySimulationHandler.KEY, new AppHolidaySimulationHandler());
    }

    private void readMasterData() {
        final File masterCert = new File(dir, "master.der");
        if (masterCert.exists()) {
            try {
                CertificateFactory certFact = CertificateFactory.getInstance("X.509");
                final Certificate certificate = certFact.generateCertificate(new FileInputStream(masterCert));

                final DeviceID id = DeviceID.fromCertificate((X509Certificate) certificate);
                getSharedPreferences(CoreConstants.FILE_SHARED_PREFS, MODE_PRIVATE).edit()
                        .putString(AppConstants.SharedPrefs.PREF_MASTER_ID, id.getIDString())
                        .commit();

                require(KeyStoreController.KEY).saveCertificate(((X509Certificate) certificate), id.getIDString());
                Log.i(getClass().getSimpleName(), "Certificate for Master " + id + " loaded:\n" + certificate);
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeSlaveCert() {
        try (final FileOutputStream os = new FileOutputStream(new File(dir, "app.der"))) {
            os.write(require(NamingManager.KEY).getOwnCertificate().getEncoded());
        } catch (IOException | CertificateEncodingException e) {
            e.printStackTrace();
        }
    }

    private void writeSlaveId() {
        try (final FileOutputStream os = new FileOutputStream(new File(dir, "app.id"))) {
            os.write(require(NamingManager.KEY).getOwnID().getId().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}