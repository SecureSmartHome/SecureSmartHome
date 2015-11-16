package de.unipassau.isl.evs.ssh.core.sec;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;

import static de.unipassau.isl.evs.ssh.core.util.UtilityFunctions.byteArrayToHexString;

/**
 * The KeyStoreController controls the KeyStore which can load and store keys and provides Key generation.
 * It also initiates the private Key which will be associated with this device if it doesn't exist yet.
 * (Notice: the Keys handled by the KeyStoreController are not the Keys from the TypedMap package.)
 */
public class KeyStoreController extends AbstractComponent {
    public Key<KeyStoreController> KEY;

    /**
     * Loads a Key from the KeyStore.
     *
     * @param alias Alias by which the Key was stored by.
     * @return Returns the associated Key.
     */
    public java.security.Key loadKey(String alias) {
        // TODO - implement KeyStoreController.loadKey
        throw new UnsupportedOperationException();
    }

    /**
     * Generates a KeyPair suited for asymmetric encryption.
     *
     * @return Returns the generated KeyPair.
     */
    private java.security.KeyPair generateKeyPair() {
        // TODO - implement KeyStoreController.generateKeyPair
        throw new UnsupportedOperationException();
    }

    /**
     * Generates a Key suited for symmetric encryption.
     *
     * @return Returns the generated Key.
     */
    private java.security.Key generateKey() {
        // TODO - implement KeyStoreController.generateKey
        throw new UnsupportedOperationException();
    }

    /**
     * Stores a Key to the KeyStore.
     *
     * @param key   The Key to be stored.
     * @param alias The alias with which the specified Key is to be associated.
     */
    public void storeKey(java.security.Key key, String alias) {
        // TODO - implement KeyStoreController.storeKey
        throw new UnsupportedOperationException();
    }

    /**
     * Calculates the fingerprint of a public key.
     * @param publicKey PublicKey to calculate fingerprint for.
     * @return Fingerprint as String.
     * @throws NoSuchAlgorithmException
     */
    public String calculatePublicKeyFingerprint(PublicKey publicKey)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return byteArrayToHexString(md.digest(publicKey.getEncoded()));
    }
}