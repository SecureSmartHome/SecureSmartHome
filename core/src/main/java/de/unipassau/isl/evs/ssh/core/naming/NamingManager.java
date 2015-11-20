package de.unipassau.isl.evs.ssh.core.naming;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.StartupException;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;

/**
 * The NamingManager class maps certificates to IDs that are unique within the SecureSmartHome
 * System. The IDs are assigned to the master, slaves and smarphones.
 */
public class NamingManager extends AbstractComponent {

    private DeviceID localDeviceID;
    private DeviceID masterID;

    private final boolean isMaster;

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
            return localDeviceID;
        }
        return masterID;
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
     * @return the id of the device
     */
    public DeviceID getLocalDeviceId() {
        return localDeviceID;
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
    public DeviceID getDeviceID(X509Certificate cert) {
        return new DeviceID(Base64.encodeToString(cert.getSignature(), Base64.DEFAULT));
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
            return getContainer().require(KeyStoreController.KEY).getCertificate(id.getId());
        } catch (UnrecoverableEntryException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new UnresolvableNamingException(e);
        }
    }

    /**
     * Updates this NamingManager. This is needed after the master's id is written to the
     * SharedPreferences.
     */
    public void update(){
        if (isMaster) {
            masterID = localDeviceID;
        } else {
            SharedPreferences prefs = getContainer().require(ContainerService.KEY_CONTEXT)
                    .getSharedPreferences(CoreConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE);

            masterID = new DeviceID(prefs.getString(CoreConstants.SharedPrefs.PREF_MASTER_ID, null));
        }
    }

    @Override
    public void init(Container container) {
        super.init(container);
        X509Certificate localCert;
        try {
            localCert = container.require(KeyStoreController.KEY).getOwnCertificate();
            localDeviceID = getDeviceID(localCert);
        } catch (UnrecoverableEntryException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new StartupException(e);
        }
        update();
    }

    @Override
    public void destroy() {
        localDeviceID = null;
        masterID = null;
        super.destroy();
    }
}
