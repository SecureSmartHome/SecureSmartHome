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

package de.unipassau.isl.evs.ssh.core.naming;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.InstrumentationTestCase;

import org.spongycastle.x509.X509V3CertificateGenerator;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;

public class NamingManagerTest extends InstrumentationTestCase {

    private static final int ASYMMETRIC_KEY_SIZE = 256;
    private static final String KEY_PAIR_ALGORITHM = "ECIES";
    private static final String KEY_PAIR_SIGNING_ALGORITHM = "SHA224withECDSA";

    private X509Certificate createCert() throws java.security.GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM);
        generator.initialize(ASYMMETRIC_KEY_SIZE);
        KeyPair keyPair = generator.generateKeyPair();

        //Check Certificate
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        certGen.setSerialNumber(BigInteger.valueOf(1L));
        certGen.setSubjectDN(new X500Principal("CN=evs"));
        certGen.setIssuerDN(new X500Principal("CN=evs"));
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setNotBefore(new Date());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 100);
        certGen.setNotAfter(calendar.getTime());
        certGen.setSignatureAlgorithm(KEY_PAIR_SIGNING_ALGORITHM);
        return certGen.generate(keyPair.getPrivate());

    }

    private DeviceID createEnvironmentAsAfterHandshake(Container container) throws GeneralSecurityException {
        Context context = container.require(ContainerService.KEY_CONTEXT);
        NamingManager naming = container.require(NamingManager.KEY);

        // Clear SharedPreferences
        SharedPreferences sharedPref = context.getSharedPreferences(CoreConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(NamingManager.PREF_MASTER_ID);
        editor.commit();

        // Generate a master certificate,
        X509Certificate certificate = createCert();
        // write master id to SharedPreferences,
        DeviceID masterId = DeviceID.fromCertificate(certificate);
        editor.putString(NamingManager.PREF_MASTER_ID, masterId.getIDString());
        editor.commit();
        // save the certificate
        container.require(KeyStoreController.KEY).saveCertificate(certificate, masterId.getIDString());

        return masterId;
    }

    private Container createDefaultEnvironment(boolean isMasterEnv) {
        Context context = getInstrumentation().getTargetContext();

        // Clear SharedPreferences
        SharedPreferences sharedPref = context.getSharedPreferences(CoreConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(NamingManager.PREF_MASTER_ID);
        editor.commit();

        // Setup environment
        SimpleContainer container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT, new ContainerService.ContextComponent(context));
        container.register(KeyStoreController.KEY, new KeyStoreController());
        container.register(NamingManager.KEY, new NamingManager(isMasterEnv));
        return container;
    }

    public void testGetMasterID() throws Exception {
        // simulate a non-master environment
        Container container = createDefaultEnvironment(false);
        NamingManager naming = container.require(NamingManager.KEY);

        // Ensure Master is still unknown
        assertFalse(naming.isMasterIDKnown());

        // Now Handshake is complete and master is known
        DeviceID masterId = createEnvironmentAsAfterHandshake(container);

        // read Master id
        assertEquals(masterId, naming.getMasterID());

        // simulate a master environment
        container = createDefaultEnvironment(true);
        naming = container.require(NamingManager.KEY);

        assertNotNull(naming.getMasterID());
        assertNotNull(naming.getOwnID());
        assertEquals(naming.getOwnID(), naming.getMasterID());
    }

    public void testGetMasterCert() throws Exception {
        // simulate a non-master environment
        Container container = createDefaultEnvironment(false);
        DeviceID masterId = createEnvironmentAsAfterHandshake(container);
        NamingManager naming = container.require(NamingManager.KEY);

        assertNotNull(naming.getMasterCertificate());

        // simulate a master environment
        container = createDefaultEnvironment(false);
        masterId = createEnvironmentAsAfterHandshake(container);
        naming = container.require(NamingManager.KEY);

        assertNotNull(naming.getMasterCertificate());
        assertNotNull(naming.getCertificate(naming.getMasterID()));
        assertEquals(naming.getMasterCertificate(), naming.getCertificate(naming.getMasterID()));
    }

    public void testGetLocalDeviceId() throws Exception {
        Container container = createDefaultEnvironment(false);
        NamingManager naming = container.require(NamingManager.KEY);
        assertNotNull(naming.getOwnID());
    }

    public void testGetDeviceID() throws Exception {
        Container container = createDefaultEnvironment(true);
        NamingManager naming = container.require(NamingManager.KEY);
        X509Certificate cert = createCert();
        assertNotNull(DeviceID.fromCertificate(cert));

    }

    public void testGetCertificate() throws Exception {
        Container container = createDefaultEnvironment(true);
        NamingManager naming = container.require(NamingManager.KEY);

        // generate and save the certificate
        X509Certificate cert = createCert();
        DeviceID id = DeviceID.fromCertificate(cert);
        container.require(KeyStoreController.KEY).saveCertificate(cert, id.getIDString());

        assertEquals(naming.getCertificate(id), cert);
    }


    public void testGetPublicKey() throws Exception {
        //
    }
}