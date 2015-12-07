package de.unipassau.isl.evs.ssh.core.sec;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.spongycastle.x509.X509V3CertificateGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.StartupException;

/**
 * The KeyStoreController controls the KeyStore which can load and store keys and provides Key generation.
 * It also initiates the private Key which will be associated with this device if it doesn't exist yet.
 * (Notice: the Keys handled by the KeyStoreController are not the Keys from the TypedMap package.)
 *
 * @author Chris
 */
public class KeyStoreController extends AbstractComponent {
    private static final String LOCAL_PRIVATE_KEY_ALIAS = "localPrivateKey";
    private static final String KEY_STORE_FILENAME = "encryptText-keystore.bks";
    private static final String KEY_STORE_TYPE = "BKS";
    private static final String KEY_PAIR_ALGORITHM = "ECIES";
    private static final String KEY_PAIR_SIGNING_ALGORITHM = "SHA224withECDSA";
    private static final String PUBLIC_KEY_PREFIX = "public_key:";
    private static final int ASYMMETRIC_KEY_SIZE = 256;


    public static final Key<KeyStoreController> KEY = new Key<>(KeyStoreController.class);

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    private KeyStore keyStore;
    private File keyStoreFile;

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

            Context containerServiceContext = container.require(ContainerService.KEY_CONTEXT);
            keyStoreFile = containerServiceContext.getFileStreamPath(KEY_STORE_FILENAME);
            if (keyStoreFile.exists()) {
                char[] keyStorePassword = getKeystorePassword();
                keyStore.load(containerServiceContext.openFileInput(KEY_STORE_FILENAME), keyStorePassword);
                Arrays.fill(keyStorePassword, (char) 0);
            } else {
                keyStore.load(null);
            }

            if (!keyStore.containsAlias(LOCAL_PRIVATE_KEY_ALIAS)) {
                generateAndStoreOwnKeyPair();
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new StartupException(e);
        }
    }

    /**
     * Returns the PrivateKey of the device using the OdroidKeyStoreController
     *
     * @return PrivateKey of this device
     */
    @NonNull
    public PrivateKey getOwnPrivateKey() {
        try {
            final KeyStore.Entry key = loadKey(LOCAL_PRIVATE_KEY_ALIAS);
            if (key instanceof KeyStore.PrivateKeyEntry) {
                return ((KeyStore.PrivateKeyEntry) key).getPrivateKey();
            } else {
                throw new StartupException("own private key not available, expected KeyStore.PrivateKeyEntry " +
                        "but got " + key + " for " + LOCAL_PRIVATE_KEY_ALIAS);
            }
        } catch (KeyStoreException | UnrecoverableEntryException | NoSuchAlgorithmException e) {
            throw new StartupException("own private key not available", e);
        }
    }

    /**
     * Returns the Certificate of the device using the OdroidKeyStoreController
     *
     * @return PrivateKey of this device
     */
    @NonNull
    public X509Certificate getOwnCertificate() {
        try {
            final KeyStore.Entry key = loadKey(LOCAL_PRIVATE_KEY_ALIAS);
            if (key instanceof KeyStore.PrivateKeyEntry) {
                return (X509Certificate) ((KeyStore.PrivateKeyEntry) key).getCertificate();
            } else {
                throw new StartupException("own private key not available, expected KeyStore.PrivateKeyEntry " +
                        "but got " + key + " for " + LOCAL_PRIVATE_KEY_ALIAS);
            }
        } catch (KeyStoreException | UnrecoverableEntryException | NoSuchAlgorithmException e) {
            throw new StartupException("own private key not available", e);
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
    @Nullable
    public X509Certificate getCertificate(String alias) throws
            UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException {
        Certificate cert = null;
        if (alias.equals(LOCAL_PRIVATE_KEY_ALIAS)) {
            KeyStore.Entry entry = loadKey(alias);
            if (entry != null) {
                cert = ((KeyStore.PrivateKeyEntry) entry).getCertificate();
            }
        } else {
            KeyStore.Entry entry = loadKey(PUBLIC_KEY_PREFIX + alias);
            if (entry != null) {
                cert = ((KeyStore.TrustedCertificateEntry) entry).getTrustedCertificate();
            }
        }
        return (X509Certificate) cert;
    }

    /**
     * Saves a certificate and adds an alias to it, so it may be found again.
     *
     * @param certificate that should be stored
     * @param alias       for the certificate that should be stored
     * @throws KeyStoreException
     */
    public void saveCertificate(X509Certificate certificate, String alias) throws
            KeyStoreException, CertificateException, NoSuchAlgorithmException {
        storeCertificate(certificate, PUBLIC_KEY_PREFIX + alias);
    }

    /**
     * Loads a KeyStore.Entry from the KeyStore.
     *
     * @param alias Alias by which the Entry was stored by.
     * @return Returns the associated keyStoreEntry or null if there is no entry stored.
     */
    @Nullable
    private KeyStore.Entry loadKey(String alias) throws
            KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException {
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
     * Stores a Key to the KeyStore.
     *
     * @param certificate The Key to be stored.
     * @param alias       The alias with which the specified Key is to be associated.
     * @throws KeyStoreException        if certificate cannot be added to the KeyStore
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    private void storeCertificate(Certificate certificate, String alias) throws KeyStoreException,
            CertificateException, NoSuchAlgorithmException {
        keyStore.setCertificateEntry(alias, certificate);

        try (FileOutputStream fileOutputStream = new FileOutputStream(keyStoreFile)) {
            char[] keyPairPassword = getKeyPairPassword();
            keyStore.store(fileOutputStream, keyPairPassword);
            Arrays.fill(keyPairPassword, (char) 0);
        } catch (IOException ex) {
            throw new KeyStoreException(ex);
        }
    }

    /**
     * Generates a KeyPair suited for asymmetric encryption.
     */
    private void generateAndStoreOwnKeyPair() throws NoSuchAlgorithmException, KeyStoreException,
            CertificateException, SignatureException, InvalidKeyException {

        String keyPairAlgorithm;

        if (KEY_PAIR_ALGORITHM.startsWith("EC")) {
            //Hardcoded Algorithms
            //see org.spongycastle.jcajce.provider.asymmetric.EC.Mappings.configure(ConfigurableProvider), line 52:
            //     "KeyPairGenerator.ECIES" -> "KeyPairGeneratorSpi$ECDH"
            keyPairAlgorithm = "EC";
            Log.w(KeyStoreController.class.getSimpleName(), "Using 'EC' instead of '" + keyPairAlgorithm
                    + "' to circumvent wrong BouncyCastle mappings");
        } else {
            keyPairAlgorithm = KEY_PAIR_ALGORITHM;
        }

        KeyPairGenerator generator;

        generator = KeyPairGenerator.getInstance(keyPairAlgorithm);
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

    public List<String> listEntries() throws KeyStoreException {
        return Collections.list(keyStore.aliases());
    }

    // TODO: generate password from device ID
    // https://stackoverflow.com/questions/2785485/is-there-a-unique-android-device-id/2853253#2853253

    private char[] getKeystorePassword() {
        return "titBewgOt2".toCharArray();
    }

    private char[] getKeyPairPassword() {
        return "clipnitLav9".toCharArray();
    }
}