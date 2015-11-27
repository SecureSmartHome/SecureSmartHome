package de.unipassau.isl.evs.ssh.core.naming;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.X509Certificate;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.StartupException;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;

/**
 * The NamingManager class maps certificates to IDs that are unique within the SecureSmartHome
 * System. The IDs are assigned to the master, slaves and smarphones.
 *
 * @author Wolfgang Popp
 */
public class NamingManager extends AbstractComponent {

    public static final Key<NamingManager> KEY = new Key<>(NamingManager.class);
    private final boolean isMaster;
    private String localDeviceID;

    /**
     * Creates a new NamingManager.
     *
     * @param isMaster describes if this NamaingManager is running on the master
     */
    public NamingManager(boolean isMaster) {
        this.isMaster = isMaster;
    }

    /**
     * Gets the master's DeviceID. If invoked on a master itself, then
     * {@code getMasterID().equals(getLocalDeviceID)}.
     *
     * @return the master's id or null if the master is not known
     */
    public DeviceID getMasterID() {
        if (isMaster) {
            return new DeviceID(localDeviceID);
        }
        SharedPreferences prefs = getContainer().require(ContainerService.KEY_CONTEXT)
                .getSharedPreferences(CoreConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE);
        String masterID = prefs.getString(CoreConstants.SharedPrefs.PREF_MASTER_ID, null);
        if (masterID == null) {
            return null;
        }
        return new DeviceID(masterID);
    }

    /**
     * Gets the master's certificate.
     *
     * @return the master's certificate or null if the certificate cannot be found.
     * @throws UnresolvableNamingException if the query fails
     */
    public X509Certificate getMasterCert() throws UnresolvableNamingException {
        return getCertificate(getMasterID());
    }

    /**
     * Gets the id of this device.
     *
     * @return the id of the local device
     */
    public DeviceID getLocalDeviceId() {
        return new DeviceID(localDeviceID);
    }


    /**
     * Gets the public key of the given DeviceID.
     *
     * @param id the id
     * @return the publicKey of the given DeviceID
     * @throws UnresolvableNamingException if the query fails
     */
    public PublicKey getPublicKey(DeviceID id) throws UnresolvableNamingException {
        return getCertificate(id).getPublicKey();
    }


    /**
     * Gets the id of a given certificate.
     *
     * @param cert the certificate
     * @return the id corresponding to the given certificate
     */
    public DeviceID getDeviceID(X509Certificate cert) throws UnresolvableNamingException {
        MessageDigest md;
        byte[] digest;

        try {
            md = MessageDigest.getInstance("SHA-256", "BC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new UnresolvableNamingException(e);
        }
        md.update(cert.getPublicKey().getEncoded());
        digest = md.digest();
        return new DeviceID(Base64.encodeToString(digest, Base64.DEFAULT));
    }

    /**
     * Gets the certificate of the given DeviceID.
     *
     * @param id the id
     * @return the certificate of the given DeviceID
     * @throws UnresolvableNamingException if the query fails
     */
    public X509Certificate getCertificate(DeviceID id) throws UnresolvableNamingException {
        try {
            if (id == null) {
                throw new UnresolvableNamingException("id == null");
            }
            return getContainer().require(KeyStoreController.KEY).getCertificate(id.getId());
        } catch (UnrecoverableEntryException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new UnresolvableNamingException(e);
        }
    }

    @Override
    public void init(Container container) {
        super.init(container);
        X509Certificate localCert;
        try {
            localCert = container.require(KeyStoreController.KEY).getOwnCertificate();
            localDeviceID = getDeviceID(localCert).getId();
        } catch (UnresolvableNamingException | UnrecoverableEntryException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new StartupException(e);
        }
    }

    @Override
    public void destroy() {
        localDeviceID = null;
        super.destroy();
    }
}
