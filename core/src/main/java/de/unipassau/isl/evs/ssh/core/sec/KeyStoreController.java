package de.unipassau.isl.evs.ssh.core.sec;

import android.content.Context;
import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.StartupException;
import org.spongycastle.x509.X509V3CertificateGenerator;
import javax.crypto.KeyGenerator;
import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

/**
 * The KeyStoreController controls the KeyStore which can load and store keys and provides Key generation.
 * It also initiates the private Key which will be associated with this device if it doesn't exist yet.
 * (Notice: the Keys handled by the KeyStoreController are not the Keys from the TypedMap package.)
 */
public class KeyStoreController extends AbstractComponent {

    public static final String LOCAL_PRIVATE_KEY_ALIAS = "localPrivateKey";
    public static final String KEY_STORE_FILENAME = "encryptText-keystore.bks";
    public static final String KEY_STORE_TYPE = "BKS";
    public static final String KEY_PAIR_ALGORITHM = "RSA";
    public static final String KEY_PAIR_SIGNING_ALGORITHM = "SHA256withRSA";
    public static final String SYMMETRIC_KEY_ALGORITHM = "AES";
    public static final String PUBLIC_KEY_PREFIX = "public_key:";
    public static final int ASYMMETRIC_KEY_SIZE = 4096;
    public static final int SYMMETRIC_KEY_SIZE = 256;

    public static final Key<KeyStoreController> KEY = new Key<>(KeyStoreController.class);
    private KeyStore keyStore;
    private File keyStoreFile;

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    /**
     * This functions allows the Container to initialize the OdroidKeyStoreController.
     *
     * @param container The Container calling this function will reference itself so that Components
     *                  know by whom they are managed.
     */
    @Override
    public void init(Container container) {
        super.init(container);

        try {
            keyStore = KeyStore.getInstance(KEY_STORE_TYPE);

            Context containerServiceContext = container.get(ContainerService.KEY_CONTEXT);
            keyStoreFile = containerServiceContext.getFileStreamPath(KEY_STORE_FILENAME);
            if (keyStoreFile.exists()) {
                char[] keyStorePassword = getKeystorePassword();
                keyStore.load(containerServiceContext.openFileInput(KEY_STORE_FILENAME), keyStorePassword);
                Arrays.fill(keyStorePassword, (char) 0);
            } else {
                keyStore.load(null);
            }

            if (!keyStore.containsAlias(LOCAL_PRIVATE_KEY_ALIAS)) {
                generateAndStoreKeyPair();
            }
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException
                | SignatureException | InvalidKeyException e) {
            throw new StartupException(e);
        }
    }

    /**
     * Returns the certificate for the associated alias or null if there is no key for this alias
     *
     * @param alias identifying the certificate
     * @return certificate for the associated alias
     * @throws UnrecoverableEntryException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    public X509Certificate getCertificate(String alias) throws UnrecoverableEntryException, NoSuchAlgorithmException,
            KeyStoreException {

        Certificate cert = null;

        if (alias.equals(LOCAL_PRIVATE_KEY_ALIAS)) {
            KeyStore.Entry entry = loadKey(alias);
            cert = ((KeyStore.PrivateKeyEntry) entry).getCertificate();
        } else {
            KeyStore.Entry entry = loadKey(PUBLIC_KEY_PREFIX + alias);
            if (entry!= null) {
                cert = ((KeyStore.TrustedCertificateEntry) entry).getTrustedCertificate();
            }
        }
        return (X509Certificate) cert;
    }

    /**
     * Returns the PrivateKey of the device using the OdroidKeyStoreController
     *
     * @return PrivateKey of this device
     * @throws UnrecoverableEntryException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    public java.security.Key getOwnPrivateKey() throws UnrecoverableEntryException, NoSuchAlgorithmException,
            KeyStoreException {
        return ((KeyStore.PrivateKeyEntry) loadKey(LOCAL_PRIVATE_KEY_ALIAS)).getPrivateKey();
    }

    /**
     * Returns the Certificate of the device using the OdroidKeyStoreController
     *
     * @return PrivateKey of this device
     * @throws UnrecoverableEntryException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    public X509Certificate getOwnCertificate() throws UnrecoverableEntryException,
            NoSuchAlgorithmException, KeyStoreException {
        return (X509Certificate) ((KeyStore.PrivateKeyEntry) loadKey(LOCAL_PRIVATE_KEY_ALIAS)).getCertificate();
    }

    /**
     * Saves a certificate and adds an alias to it, so it may be found again.
     *
     * @param certificate that should be stored
     * @param alias for the certificate that should be stored
     * @throws KeyStoreException
     */
    public void saveCertifcate(X509Certificate certificate, String alias) throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException{
        storeKey(certificate, PUBLIC_KEY_PREFIX + alias);
    }

    /**
     * Loads a KeyStore.Entry from the KeyStore.
     *
     * @param alias Alias by which the Entry was stored by.
     * @return Returns the associated keyStoreEntry or null if there is no entry stored.
     */
    private KeyStore.Entry loadKey(String alias) throws KeyStoreException, UnrecoverableEntryException,
            NoSuchAlgorithmException {

        KeyStore.Entry entry;
        if (keyStore.containsAlias(alias)) {
            char[] keyPairPassword = getKeyPairPassword();
            entry = keyStore.getEntry(alias, new KeyStore.PasswordProtection(keyPairPassword));
            Arrays.fill(keyPairPassword, (char) 0);
            return entry;
        }

        return null;
    }

    /**
     * Generates a KeyPair suited for asymmetric encryption.
     */
    private void generateAndStoreKeyPair() throws NoSuchAlgorithmException, KeyStoreException,
            CertificateException, SignatureException, InvalidKeyException {

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

        char[] keyPairPassword = getKeyPairPassword();

        keyStore.setEntry(LOCAL_PRIVATE_KEY_ALIAS,
                new KeyStore.PrivateKeyEntry(keyPair.getPrivate(), new X509Certificate[]{cert}),
                new KeyStore.PasswordProtection(keyPairPassword));

        Arrays.fill(keyPairPassword, (char) 0);

        try (FileOutputStream fileOutputStream = new FileOutputStream(keyStoreFile)) {
            keyStore.store(fileOutputStream, getKeystorePassword());
        } catch (IOException ex) {
            throw new KeyStoreException(ex);
        }
    }

    /**
     * Generates a Key suited for symmetric encryption.
     *
     * @return Returns the generated Key.
     *
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     */
    public java.security.Key generateKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance(SYMMETRIC_KEY_ALGORITHM);
        generator.init(SYMMETRIC_KEY_SIZE);
        return KeyGenerator.getInstance(SYMMETRIC_KEY_ALGORITHM).generateKey();
    }

    /**
     * Stores a Key to the KeyStore.
     *
     * @param certificate   The Key to be stored.
     * @param alias The alias with which the specified Key is to be associated.
     *
     * @throws KeyStoreException if certificate cannot be added to the KeyStore
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    private void storeKey(Certificate certificate, String alias) throws KeyStoreException,
            CertificateException, NoSuchAlgorithmException {
        keyStore.setCertificateEntry(alias, certificate);

        try (FileOutputStream fileOutputStream = new FileOutputStream(keyStoreFile)) {
            char[] keyStorePassword = getKeystorePassword();
            keyStore.store(fileOutputStream, keyStorePassword);
            Arrays.fill(keyStorePassword, (char) 0);
        } catch (IOException ex) {
            throw new KeyStoreException(ex);
        }
    }

    private char[] getKeystorePassword() {
        return "titBewgOt2".toCharArray();
    }

    private char[] getKeyPairPassword() {
        return "clipnitLav9".toCharArray();
    }
}