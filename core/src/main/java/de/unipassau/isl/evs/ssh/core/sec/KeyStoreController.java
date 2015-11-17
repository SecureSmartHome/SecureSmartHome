package de.unipassau.isl.evs.ssh.core.sec;


import android.content.Context;
import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import org.spongycastle.x509.X509V3CertificateGenerator;
import javax.crypto.KeyGenerator;
import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
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
    public static final String KEY_PAIR_ALGORITHM = "ECDHC";
    private static final String KEY_PAIR_SIGNING_ALGORITHM = "SHA256WITHDETECDSA";
    private static final String SYMMETRIC_KEY_ALGORITHM = "BLOWFISH";
    public static final int KEY_SIZE = 1024;

    public static final Key<KeyStoreController> KEY = new Key<>(KeyStoreController.class);
    private KeyStore keyStore;

    @Override
    public void init(Container container) {
        super.init(container);

        try {
            keyStore = KeyStore.getInstance(KEY_STORE_TYPE);

            Context containerServiceContext = container.get(ContainerService.KEY_CONTEXT);
            File file = containerServiceContext.getFileStreamPath(KEY_STORE_FILENAME);
            if (file.exists()) {
                char[] keyStorePassword = getKeystorePassword();
                keyStore.load(containerServiceContext.openFileInput(KEY_STORE_FILENAME), keyStorePassword);
                keyStorePassword = null; //set to null to remove the password from memory
            } else {
                keyStore.load(null);
            }

            if (!keyStore.containsAlias(LOCAL_PRIVATE_KEY_ALIAS)) {
                generateKeyPair();
            }
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException
                | SignatureException | InvalidKeyException e) {
            throw new RuntimeException();
        }
    }

    /**
     * Loads a Key from the KeyStore.
     *
     * @param alias Alias by which the Key was stored by.
     * @return Returns the associated Key or null if there is no key stored.
     */
    public java.security.Key loadKey(String alias) throws KeyStoreException, UnrecoverableEntryException,
            NoSuchAlgorithmException {

        KeyStore.Entry entry;
        if (keyStore.containsAlias(alias)) {
            entry = keyStore.getEntry(alias, null);

            java.security.Key key;

            if (alias.equals(LOCAL_PRIVATE_KEY_ALIAS)) {
                key = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
            } else {
                key = ((KeyStore.PrivateKeyEntry) entry).getCertificate().getPublicKey();
            }
            return key;
        }

        return null;
    }

    /**
     * Generates a KeyPair suited for asymmetric encryption.
     *
     * @return Returns the generated KeyPair.
     */
    private java.security.KeyPair generateKeyPair() throws NoSuchAlgorithmException, KeyStoreException,
            CertificateEncodingException, SignatureException, InvalidKeyException {

        KeyPairGenerator generator;

        generator = KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM);
        generator.initialize(KEY_SIZE);

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
        certGen.setSignatureAlgorithm(KEY_PAIR_SIGNING_ALGORITHM);
        X509Certificate cert = certGen.generate(keyPair.getPrivate());

        keyStore.setEntry(LOCAL_PRIVATE_KEY_ALIAS,
                new KeyStore.PrivateKeyEntry(keyPair.getPrivate(), new X509Certificate[]{cert}),
                null);

        return keyPair;
    }

    /**
     * Generates a Key suited for symmetric encryption.
     *
     * @return Returns the generated Key.
     *
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     */
    private java.security.Key generateKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
        return KeyGenerator.getInstance(SYMMETRIC_KEY_ALGORITHM).generateKey();
    }

    /**
     * Stores a Key to the KeyStore.
     *
     * @param certificate   The Key to be stored.
     * @param alias The alias with which the specified Key is to be associated.
     *
     * @throws KeyStoreException if certificate cannot be added to the KeyStore
     */
    public void storeKey(Certificate certificate, String alias) throws KeyStoreException {
        keyStore.setCertificateEntry(alias, certificate);
    }

    private char[] getKeystorePassword() {
        return "titBewgOt2".toCharArray();
    }
}