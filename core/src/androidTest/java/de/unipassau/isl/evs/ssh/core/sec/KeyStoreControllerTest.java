package de.unipassau.isl.evs.ssh.core.sec;

import android.test.InstrumentationTestCase;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import org.spongycastle.x509.X509V3CertificateGenerator;

import javax.crypto.Cipher;
import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

/**
 * Instrumentation Test for the KeyStoreController
 */
public class KeyStoreControllerTest extends InstrumentationTestCase {
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

        container.register(controller.KEY, controller);
        purgeKeyStore(controller);
        container.unregister(controller.KEY);
        container.register(controller.KEY, controller);
    }

    /**
     * Test method for the initialization of the KeyStoreController
     *
     * @throws Exception
     */
    public void  testLoadKey() throws Exception {
        SimpleContainer container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(getInstrumentation().getTargetContext()));

        KeyStoreController controller = new KeyStoreController();

        container.register(controller.KEY, controller);
        purgeKeyStore(controller);
        container.unregister(controller.KEY);
        container.register(controller.KEY, controller);

        Certificate nonExCert = controller.getCertificate("TestNonExistent");
        assertNull(nonExCert);

        Key publicKey = controller.getCertificate(KeyStoreController.LOCAL_PRIVATE_KEY_ALIAS).getPublicKey();
        Key privateKey = controller.getOwnPrivateKey();
        assertNotNull(publicKey);
        assertNotNull(privateKey);

        Cipher cipher = Cipher.getInstance(KeyStoreController.KEY_PAIR_ALGORITHM);

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

        container.register(controller.KEY, controller);
        purgeKeyStore(controller);
        container.unregister(controller.KEY);
        container.register(controller.KEY, controller);

        Key privateKey = controller.getOwnPrivateKey();

        Key publicKey = controller.getOwnCertificate().getPublicKey();

        assertNotNull(publicKey);
        assertNotNull(privateKey);

        Cipher cipher = Cipher.getInstance(KeyStoreController.KEY_PAIR_ALGORITHM);

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

        container.register(controller.KEY, controller);

        KeyPairGenerator generator;

        generator = KeyPairGenerator.getInstance(KeyStoreController.KEY_PAIR_ALGORITHM);
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
        X509Certificate cert = certGen.generate(keyPair.getPrivate());

        controller.saveCertifcate(cert, "TestAlias");

        container.unregister(controller.KEY);
        controller = null;
        controller = new KeyStoreController();
        container.register(controller.KEY, controller);

        assertTrue(Arrays.equals(controller.getCertificate("TestAlias").getEncoded(), cert.getEncoded()));
    }

    /**
     * Test method for the generating symmetric keys
     *
     * @throws Exception
     */
    public void testGenerateSymmetricKey() throws Exception {
        SimpleContainer container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(getInstrumentation().getTargetContext()));
        KeyStoreController controller = new KeyStoreController();

        container.register(controller.KEY, controller);
        purgeKeyStore(controller);
        container.unregister(controller.KEY);
        container.register(controller.KEY, controller);

        assertNotNull(controller.generateKey());

        Key key = controller.generateKey();
        Cipher cipher = Cipher.getInstance(KeyStoreController.SYMMETRIC_KEY_ALGORITHM);

        String input = "Test-String";
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(input.getBytes());

        cipher.init(Cipher.DECRYPT_MODE, key);
        String decrypted = new String(cipher.doFinal(encrypted));

        assertEquals(input, decrypted);
    }

    private void purgeKeyStore(KeyStoreController controller) throws KeyStoreException {
        for (String s : controller.listEntries()) {
            controller.deleteKeyStoreEntry(s);
        }
    }
}