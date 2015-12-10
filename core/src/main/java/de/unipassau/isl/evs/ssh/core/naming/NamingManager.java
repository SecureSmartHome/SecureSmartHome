package de.unipassau.isl.evs.ssh.core.naming;


import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;

/**
 * The NamingManager class maps certificates to IDs that are unique within the SecureSmartHome
 * System. The IDs are assigned to the master, slaves and smarphones.
 *
 * @author Wolfgang Popp
 */
public class NamingManager extends AbstractComponent {
    public static final Key<NamingManager> KEY = new Key<>(NamingManager.class);
    public static final String PREF_MASTER_ID = "ssh.core.MASTER_ID";
    private static final String TAG = NamingManager.class.getSimpleName();

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

    public boolean isMaster() {
        return isMaster;
    }

    /**
     * Gets the master's DeviceID. If invoked on a master itself, then
     * {@code getMasterID().equals(getLocalDeviceID)}.
     *
     * @return the master's id
     * @throws IllegalStateException if the Master ID is not known
     */
    @NonNull
    public DeviceID getMasterID() {
        if (masterID == null) {
            try {
                loadMasterData();
            } catch (UnresolvableNamingException e) {
                throw new IllegalStateException("Could not load Master Certificate", e);
            }
        }
        if (masterID == null) {
            throw new IllegalStateException("Master ID not known");
        }
        return masterID;
    }

    /**
     * Gets the master's certificate.
     *
     * @return the master's certificate
     * @throws IllegalStateException if the Master Certificate is not known
     */
    @NonNull
    public X509Certificate getMasterCertificate() {
        if (masterCert == null) {
            try {
                loadMasterData();
            } catch (UnresolvableNamingException e) {
                throw new IllegalStateException("Could not load Master Certificate", e);
            }
        }
        if (masterCert == null) {
            throw new IllegalStateException("Master ID not known");
        }
        return masterCert;
    }

    /**
     * @return {@code true}, if the ID of the Master is known, i.e. it is stored in the SharedPreferences
     */
    public boolean isMasterIDKnown() {
        if (isMaster || masterID != null) {
            return true;
        }
        try {
            loadMasterData();
        } catch (UnresolvableNamingException ignore) {
        }
        return (masterID != null);
    }

    /**
     * @return {@code true}, if the ID and Certificate of the Master are known, i.e. they are stored in the
     * SharedPreferences and the KeyStore respectively
     */
    public boolean isMasterKnown() {
        if (isMaster || (masterID != null && masterCert != null)) {
            return true;
        }
        try {
            loadMasterData();
        } catch (UnresolvableNamingException e) {
            return false;
        }
        return (masterID != null && masterCert != null);
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
     * @deprecated use {@link DeviceID#fromCertificate(X509Certificate)} instead
     */
    @Deprecated
    @NonNull
    public DeviceID getDeviceID(X509Certificate cert) throws UnresolvableNamingException {
        return DeviceID.fromCertificate(cert);
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
            } else if (id.equals(getOwnID())) {
                return getOwnCertificate();
            }
            final X509Certificate certificate = requireComponent(KeyStoreController.KEY).getCertificate(id.getIDString());
            if (certificate == null) {
                throw new UnresolvableNamingException("Certificate for Device " + id + " not found");
            }
            return certificate;
        } catch (UnrecoverableEntryException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new UnresolvableNamingException(e);
        }
    }

    @Override
    public void init(Container container) {
        super.init(container);

        final KeyStoreController keyStoreController = getContainer().require(KeyStoreController.KEY);
        ownCert = keyStoreController.getOwnCertificate();
        ownID = DeviceID.fromCertificate(ownCert);

        if (isMaster) {
            masterCert = ownCert;
            masterID = ownID;
        } else {
            try {
                loadMasterData();
            } catch (UnresolvableNamingException ignore) {
            }
        }
    }

    private void loadMasterData() throws UnresolvableNamingException {
        final SharedPreferences prefs = requireComponent(ContainerService.KEY_CONTEXT)
                .getSharedPreferences(CoreConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE);

        String masterIDStr = prefs.getString(CoreConstants.SharedPrefs.PREF_MASTER_ID, null);
        if (masterIDStr != null) {
            masterID = new DeviceID(masterIDStr);
            masterCert = getCertificate(masterID);
        }
    }

    public void setMasterID(DeviceID masterID) {
        if (masterID == null) throw new NullPointerException("masterID");
        if (this.masterID != null) throw new IllegalStateException("masterID already known");

        requireComponent(ContainerService.KEY_CONTEXT)
                .getSharedPreferences(CoreConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(CoreConstants.SharedPrefs.PREF_MASTER_ID, masterID.getIDString())
                .commit();

        this.masterID = masterID;
    }

    public void setMasterCertificate(X509Certificate masterCert) throws CertificateException, NoSuchAlgorithmException, KeyStoreException {
        if (masterCert == null) throw new NullPointerException("masterCert");
        if (this.masterCert != null) throw new IllegalStateException("masterCert already known");
        final DeviceID certID = DeviceID.fromCertificate(masterCert);
        if (masterID != null && !masterID.equals(certID)) {
            throw new CertificateException("MasterID generated from Certificate " + certID + " does not match " +
                    "already known MasterID " + masterID);
        }

        if (masterID == null) {
            setMasterID(certID);
        }

        requireComponent(KeyStoreController.KEY).saveCertificate(masterCert, masterID.getIDString());

        this.masterCert = masterCert;
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
