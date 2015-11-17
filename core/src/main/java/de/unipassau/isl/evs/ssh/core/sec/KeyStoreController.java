package de.unipassau.isl.evs.ssh.core.sec;


import android.content.Context;
import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import org.spongycastle.x509.X509V3CertificateGenerator;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
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
import java.security.spec.KeySpec;
import java.util.Calendar;
import java.util.Date;

/**
 * The KeyStoreController controls the KeyStore which can load and store keys and provides Key generation.
 * It also initiates the private Key which will be associated with this device if it doesn't exist yet.
 * (Notice: the Keys handled by the KeyStoreController are not the Keys from the TypedMap package.)
 */
public class KeyStoreController extends AbstractComponent {
    public static final char[] KEYSTORE_PASSWORD = new char[]{'a', 'b', 'c'}; //FIXME, where do we get that from

    public static final String LOCAL_PRIVATE_KEY_ALIAS = "localPrivateKey";
    public static final String KEY_STORE_FILENAME = "encryptText-keystore.bks";
    public static final String KEY_STORE_TYPE = "BKS";
    public static final String KEY_PAIR_ALGORITHM = "RSA";
    private static final String KEY_PAIR_SIGNING_ALGORITHM = "SHA256withRSA";
    public static final int KEY_SIZE = 1024;

    public Key<KeyStoreController> KEY;
    private KeyStore keyStore;

    @Override
    public void init(Container container) {
        super.init(container);

        try {
            keyStore = KeyStore.getInstance(KEY_STORE_TYPE);

            Context containerServiceContext = container.get(ContainerService.KEY_CONTEXT);
            File file = containerServiceContext.getFileStreamPath(KEY_STORE_FILENAME);
            if (file.exists()) {
                keyStore.load(containerServiceContext.openFileInput(KEY_STORE_FILENAME), KEYSTORE_PASSWORD);
            } else {
                keyStore.load(null);
            }

            if (!keyStore.containsAlias(LOCAL_PRIVATE_KEY_ALIAS)) {
                generateKeyPair();
            }

        } catch (KeyStoreException e) {
            //TODO: Which Exception should be thrown here?
            //This seems pretty critical as we cannot communicate without the keystore, right?
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads a Key from the KeyStore.
     *
     * @param alias Alias by which the Key was stored by.
     * @return Returns the associated Key.
     */
    public java.security.Key loadKey(String alias) {

        KeyStore.Entry entry;
        try {

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
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableEntryException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        return null; //TODO: Is this ok? if not what else
    }

    /**
     * Generates a KeyPair suited for asymmetric encryption.
     *
     * @return Returns the generated KeyPair.
     */
    private java.security.KeyPair generateKeyPair() {
        KeyPairGenerator generator;

        try {
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

            //FIXME KeyPairPassword???
            keyStore.setEntry(LOCAL_PRIVATE_KEY_ALIAS,
                    new KeyStore.PrivateKeyEntry(keyPair.getPrivate(), new X509Certificate[]{cert}),
                    null);

            return keyPair;

        //TODO: What do we do with exceptions
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Generates a Key suited for symmetric encryption.
     *
     * @return Returns the generated Key.
     */
    private java.security.Key generateKey() {
        //TODO: What password?
        String password = "SoThatItCompilesDefinatelyChangeThis_o.O";

        SecretKeyFactory keyFactory;
        SecretKey key = null;

        try {
            keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), new byte[]{'a', 'b', 'c'}, 100, 256);
            key = keyFactory.generateSecret(keySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return key;
    }

    /**
     * Stores a Key to the KeyStore.
     *
     * @param certificate   The Key to be stored.
     * @param alias The alias with which the specified Key is to be associated.
     */
    public void storeKey(Certificate certificate, String alias) {
        //TODO Change Request for Method signature

        try {
            keyStore.setCertificateEntry(alias, certificate);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }

}