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

package de.unipassau.isl.evs.ssh.core.sec;

import android.test.InstrumentationTestCase;

import org.spongycastle.x509.X509V3CertificateGenerator;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.Cipher;
import javax.security.auth.x500.X500Principal;

import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;

import static de.unipassau.isl.evs.ssh.core.sec.KeyStoreController.KEY;

/**
 * Instrumentation Test for the KeyStoreController
 */
public class KeyStoreControllerTest extends InstrumentationTestCase {
    private static final String LOCAL_PRIVATE_KEY_ALIAS = "localPrivateKey";
    private static final String KEY_STORE_FILENAME = "encryptText-keystore.bks";
    private static final String KEY_PAIR_ALGORITHM = "ECIES";
    private static final String KEY_PAIR_SIGNING_ALGORITHM = "SHA224withECDSA";
    private static final int ASYMMETRIC_KEY_SIZE = 256;

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    /**
     * Test method for the initialization of the KeyStoreController
     *
     * @throws Exception
     */
    public void testInit() throws Exception {
        SimpleContainer container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(getInstrumentation().getTargetContext()));
        KeyStoreController controller = new KeyStoreController();

        container.register(KEY, controller);
        purgeKeyStore();
        container.unregister(KEY);
        container.register(KEY, controller);
    }

    /**
     * Test method for the initialization of the KeyStoreController
     *
     * @throws Exception
     */
    public void testLoadKey() throws Exception {
        SimpleContainer container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(getInstrumentation().getTargetContext()));

        KeyStoreController controller = new KeyStoreController();

        container.register(KEY, controller);
        purgeKeyStore();
        container.unregister(KEY);
        container.register(KEY, controller);

        Certificate nonExCert = controller.getCertificate("TestNonExistent");
        assertNull(nonExCert);

        Key publicKey = controller.getCertificate(LOCAL_PRIVATE_KEY_ALIAS).getPublicKey();
        Key privateKey = controller.getOwnPrivateKey();
        assertNotNull(publicKey);
        assertNotNull(privateKey);

        Cipher cipher = Cipher.getInstance(KEY_PAIR_ALGORITHM);

        String input = "Test-String";
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(input.getBytes());

        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        String decrypted = new String(cipher.doFinal(encrypted));

        assertEquals(input, decrypted);
    }

    public void testOwnCertificateAndKey() throws Exception {
        SimpleContainer container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(getInstrumentation().getTargetContext()));

        KeyStoreController controller = new KeyStoreController();

        container.register(KEY, controller);
        purgeKeyStore();
        container.unregister(KEY);
        container.register(KEY, controller);

        Key privateKey = controller.getOwnPrivateKey();

        Key publicKey = controller.getOwnCertificate().getPublicKey();

        assertNotNull(publicKey);
        assertNotNull(privateKey);

        Cipher cipher = Cipher.getInstance(KEY_PAIR_ALGORITHM);

        String input = "Test-String";
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(input.getBytes());

        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        String decrypted = new String(cipher.doFinal(encrypted));

        assertEquals(input, decrypted);
    }

    /**
     * Test method for the initialization of the KeyStoreController
     *
     * @throws Exception
     */
    public void testStoreKey() throws Exception {
        SimpleContainer container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(getInstrumentation().getTargetContext()));
        KeyStoreController controller = new KeyStoreController();

        container.register(KEY, controller);

        KeyPairGenerator generator;

        generator = KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM);
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
        X509Certificate cert = certGen.generate(keyPair.getPrivate());

        controller.saveCertificate(cert, "TestAlias");

        container.unregister(KEY);
        controller = new KeyStoreController();
        container.register(KEY, controller);

        assertTrue(Arrays.equals(controller.getCertificate("TestAlias").getEncoded(), cert.getEncoded()));
    }

    private void purgeKeyStore() {
        getInstrumentation().getTargetContext().deleteFile(KEY_STORE_FILENAME);
    }
}