package de.unipassau.isl.evs.ssh.core.naming;


import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Base64;

import java.security.GeneralSecurityException;
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
    private DeviceID ownID;
    private DeviceID masterID;
    private X509Certificate ownCert;
    private X509Certificate masterCert;

    /**
     * Creates a new NamingManager.
     *
     * @param isMaster describes if this NamingManager is running on the master
     */
    public NamingManager(boolean isMaster) {
        this.isMaster = isMaster;
    }

    /**
     * Gets the master's DeviceID. If invoked on a master itself, then
     * {@code getMasterID().equals(getLocalDeviceID)}.
     *
     * @return the master's id
     */
    @NonNull
    public DeviceID getMasterID() {
        return masterID;
    }

    /**
     * Gets the master's certificate.
     *
     * @return the master's certificate
     */
    @NonNull
    public X509Certificate getMasterCertificate() {
        return masterCert;
    }

    /**
     * Gets the id of this device.
     *
     * @return the id of the local device
     */
    @NonNull
    public DeviceID getOwnID() {
        return ownID;
    }

    /**
     * Gets the local devices's certificate.
     *
     * @return the local devices's certificate
     */
    @NonNull
    public X509Certificate getOwnCertificate() {
        return ownCert;
    }

    public boolean isMaster() {
        return isMaster;
    }

    /**
     * Gets the public key of the given DeviceID.
     *
     * @param id the id
     * @return the publicKey of the given DeviceID
     * @throws UnresolvableNamingException if the query fails
     */
    @NonNull
    public PublicKey getPublicKey(DeviceID id) throws UnresolvableNamingException {
        return getCertificate(id).getPublicKey();
    }

    /**
     * Gets the id of a given certificate.
     *
     * @param cert the certificate
     * @return the id corresponding to the given certificate
     */
    @NonNull
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
        return new DeviceID(Base64.encodeToString(digest, Base64.NO_WRAP));
    }

    /**
     * Gets the certificate of the given DeviceID.
     *
     * @param id the id
     * @return the certificate of the given DeviceID
     * @throws UnresolvableNamingException if the query fails
     */
    @NonNull
    public X509Certificate getCertificate(DeviceID id) throws UnresolvableNamingException {
        try {
            if (id == null) {
                throw new UnresolvableNamingException("id == null");
            }
            final X509Certificate certificate = getContainer().require(KeyStoreController.KEY).getCertificate(id.getId());
            if (certificate == null) {
                throw new UnresolvableNamingException("certificate not found");
            }
            return certificate;
        } catch (UnrecoverableEntryException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new UnresolvableNamingException(e);
        }
    }

    @Override
    public void init(Container container) {
        super.init(container);
        try {
            final KeyStoreController keyStoreController = container.require(KeyStoreController.KEY);
            ownCert = keyStoreController.getOwnCertificate();
            ownID = getDeviceID(ownCert);

            if (isMaster) {
                masterCert = ownCert;
                masterID = ownID;
            } else {
                final SharedPreferences prefs = getContainer().require(ContainerService.KEY_CONTEXT)
                        .getSharedPreferences(CoreConstants.NettyConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE);

                String masterIDStr = prefs.getString(CoreConstants.SharedPrefs.PREF_MASTER_ID, null);
                if (masterIDStr == null) {
                    throw new StartupException("MasterID from SharedPrefs (" + CoreConstants.SharedPrefs.PREF_MASTER_ID + ") is null");
                }
                masterID = new DeviceID(masterIDStr);
                masterCert = getCertificate(masterID);
            }
        } catch (UnresolvableNamingException | GeneralSecurityException e) {
            throw new StartupException(e);
        }
    }

    @Override
    public void destroy() {
        ownID = null;
        masterID = null;
        ownCert = null;
        masterCert = null;
        super.destroy();
    }
}
