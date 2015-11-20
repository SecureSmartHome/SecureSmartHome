package de.unipassau.isl.evs.ssh.core.naming;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.InstrumentationTestCase;

import junit.framework.TestCase;

import org.spongycastle.x509.X509V3CertificateGenerator;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.R;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;

public class NamingManagerTest extends InstrumentationTestCase {

    private X509Certificate createCert() throws java.security.GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyStoreController.KEY_PAIR_ALGORITHM);
        generator.initialize(KeyStoreController.ASYMMETRIC_KEY_SIZE);
        KeyPair keyPair = generator.generateKeyPair();

        //Check Certificate
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        certGen.setSerialNumber(BigInteger.valueOf(1L));
        certGen.setSubjectDN(new X500Principal("CN=evs")); //FIXME
        certGen.setIssuerDN(new X500Principal("CN=evs"));
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setNotBefore(new Date());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 100);
        certGen.setNotAfter(calendar.getTime());
        certGen.setSignatureAlgorithm(KeyStoreController.KEY_PAIR_SIGNING_ALGORITHM);
        return  certGen.generate(keyPair.getPrivate());

    }

    private DeviceID createEnvironmentAsAfterHandshake(Container container) throws GeneralSecurityException, UnresolvableNamingException {
        Context context = container.require(ContainerService.KEY_CONTEXT);
        NamingManager naming = container.require(NamingManager.KEY);

        // Clear SharedPreferences
        SharedPreferences sharedPref = context.getSharedPreferences(CoreConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(CoreConstants.SharedPrefs.PREF_MASTER_ID);
        editor.commit();

        // Generate a master certificate,
        X509Certificate certificate = createCert();
        // write master id to SharedPreferences,
        DeviceID masterId = naming.getDeviceID(certificate);
        editor.putString(CoreConstants.SharedPrefs.PREF_MASTER_ID, masterId.getId());
        editor.commit();
        // save the certificate
        container.require(KeyStoreController.KEY).saveCertifcate(certificate, masterId.getId());

        return masterId;
    }

    private Container createDefaultEnvironment(boolean isMasterEnv){
        // Setup environment
        SimpleContainer container = new SimpleContainer();
        Context context = getInstrumentation().getTargetContext();
        container.register(ContainerService.KEY_CONTEXT, new ContainerService.ContextComponent(context));
        container.register(KeyStoreController.KEY, new KeyStoreController());
        container.register(NamingManager.KEY, new NamingManager(isMasterEnv));

        // Clear SharedPreferences
        SharedPreferences sharedPref = context.getSharedPreferences(CoreConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(CoreConstants.SharedPrefs.PREF_MASTER_ID);
        editor.commit();

        return container;
    }

    public void testGetMasterID() throws Exception {
        // simulate a non-master environment
        Container container = createDefaultEnvironment(false);
        NamingManager naming = container.require(NamingManager.KEY);

        // Ensure Master is still unknown
        assertNull(naming.getMasterID());

        // Now Handshake is complete and master is known
        DeviceID masterId = createEnvironmentAsAfterHandshake(container);

        // read Master id
        assertEquals(masterId, naming.getMasterID());

        // simulate a master environment
        container = createDefaultEnvironment(true);
        naming = container.require(NamingManager.KEY);

        assertNotNull(naming.getMasterID());
        assertNotNull(naming.getLocalDeviceId());
        assertEquals(naming.getLocalDeviceId(), naming.getMasterID());
    }

    public void testGetMasterCert() throws Exception {
        // simulate a non-master environment
        Container container = createDefaultEnvironment(false);
        DeviceID masterId = createEnvironmentAsAfterHandshake(container);
        NamingManager naming = container.require(NamingManager.KEY);

        assertNotNull(naming.getMasterCert());

        // simulate a master environment
        container = createDefaultEnvironment(false);
        masterId = createEnvironmentAsAfterHandshake(container);
        naming = container.require(NamingManager.KEY);

        assertNotNull(naming.getMasterCert());
        assertNotNull(naming.getCertificate(naming.getMasterID()));
        assertEquals(naming.getMasterCert(), naming.getCertificate(naming.getMasterID()));
    }

    public void testGetLocalDeviceId() throws Exception {
        Container container = createDefaultEnvironment(false);
        NamingManager naming = container.require(NamingManager.KEY);
        assertNotNull(naming.getLocalDeviceId());
    }

    public void testGetDeviceID() throws Exception {
        Container container = createDefaultEnvironment(true);
        NamingManager naming = container.require(NamingManager.KEY);
        X509Certificate cert = createCert();
        assertNotNull(naming.getDeviceID(cert));

    }

    public void testGetCertificate() throws Exception {
        Container container = createDefaultEnvironment(true);
        NamingManager naming = container.require(NamingManager.KEY);

        // generate and save the certificate
        X509Certificate cert = createCert();
        DeviceID id = naming.getDeviceID(cert);
        container.require(KeyStoreController.KEY).saveCertifcate(cert, id.getId());

        assertEquals(naming.getCertificate(id), cert);
    }


    public void testGetPublicKey() throws Exception {
        //
    }
}