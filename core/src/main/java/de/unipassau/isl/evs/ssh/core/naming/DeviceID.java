package de.unipassau.isl.evs.ssh.core.naming;

import android.util.Base64;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * Unique id for all devices (user devices, master, slaves).
 *
 * @author Wolfgang Popp
 */
public final class DeviceID implements Serializable {
    private static final String ID_MD_ALG = "SHA-256";

    /**
     * The length of a device id in bytes
     */
    public static final int ID_LENGTH = 32;

    private static final byte[] NO_DEVICE_BYTES = new byte[ID_LENGTH];

    static {
        Arrays.fill(NO_DEVICE_BYTES, ((byte) 0));
    }

    /**
     * A device id for a dummy device. Can be used if no device is available but a device id is needed. This approach
     * fixes a bootstrapping problem during the very first handshake.
     */
    public static final DeviceID NO_DEVICE = new DeviceID(NO_DEVICE_BYTES);

    private final String id;
    private final byte[] bytes;

    /**
     * Creates a new DeviceID from the given string.
     *
     * @param id the id as string
     */
    public DeviceID(String id) {
        this.id = id.trim();
        this.bytes = Base64.decode(id, Base64.DEFAULT);
        validateLength();
    }

    /**
     * Constructs a new DeviceID from the given byte array.
     *
     * @param bytes a
     */
    public DeviceID(byte[] bytes) {
        this.id = Base64.encodeToString(bytes, Base64.NO_WRAP).trim();
        this.bytes = Arrays.copyOf(bytes, bytes.length);
        validateLength();
    }

    private void validateLength() {
        if (bytes.length != ID_LENGTH) {
            throw new IllegalArgumentException("ID '" + id + "' has invalid length " + bytes.length + "!=" + ID_LENGTH);
        }
    }

    /**
     * Creates a new Device id from the given certificate.
     *
     * @param cert the certificate to create the id from
     * @return the device id corresponding to the certificate
     */
    public static DeviceID fromCertificate(X509Certificate cert) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(ID_MD_ALG, "SC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(ID_MD_ALG + " is not available from SpongyCastle", e);
        }
        if (md.getDigestLength() != ID_LENGTH) {
            throw new AssertionError("Message digest " + ID_MD_ALG + " returns invalid length " + md.getDigestLength() + "!=" + ID_LENGTH);
        }
        md.update(cert.getPublicKey().getEncoded());
        byte[] digest = md.digest();
        return new DeviceID(digest);
    }

    /**
     * Returns the device id string.
     *
     * @return the device id
     * @deprecated use {@link #getIDString()} instead
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the device id string.
     *
     * @return the device id
     */
    public String getIDString() {
        return id;
    }

    /**
     * Gets a byte representation of this device id.
     *
     * @return the byte representation of this id
     */
    public byte[] getIDBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DeviceID deviceID = (DeviceID) o;
        return Arrays.equals(bytes, deviceID.bytes);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Returns a short description of this device id.
     *
     * @return the short description
     */
    public String toShortString() {
        return id.substring(0, Math.min(id.length(), 7));
    }

    @Override
    public String toString() {
        return getIDString();
    }
}
