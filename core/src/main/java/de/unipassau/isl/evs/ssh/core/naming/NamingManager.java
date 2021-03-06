/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unipassau.isl.evs.ssh.core.naming;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;

/**
 * The NamingManager class maps certificates to IDs that are unique within the SecureSmartHome
 * System. The IDs are assigned to the master, slaves and smartphones.
 *
 * @author Wolfgang Popp
 */
public class NamingManager extends AbstractComponent {
    public static final Key<NamingManager> KEY = new Key<>(NamingManager.class);
    static final String PREF_MASTER_ID = "ssh.core.MASTER_ID";

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
     * Checks whether or not a this NamingManager is running on the master
     *
     * @return true if running on master
     */
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
     * Sets the master id. On client devices the master id is unknown until the handshake completed successfully. When
     * the handshake completed the id can be set with this method.
     *
     * @param masterID the master's id
     */
    public void setMasterID(DeviceID masterID) {
        if (masterID == null) throw new NullPointerException("masterID");
        if (this.masterID != null) throw new IllegalStateException("masterID already known");

        requireComponent(ContainerService.KEY_CONTEXT)
                .getSharedPreferences()
                .edit()
                .putString(PREF_MASTER_ID, masterID.getIDString())
                .commit();

        this.masterID = masterID;
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
     * Sets the master's certificate. On client devices the master certificate is unknown on first startup. During the
     * handshake the master's certificate is received. When the handshake completed the certificate can be saved with
     * this method.
     *
     * @param masterCert the master's id
     */
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

    /**
     * Checks whether master is known.
     *
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

        final KeyStoreController keyStoreController = requireComponent(KeyStoreController.KEY);
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
        final SharedPreferences prefs = requireComponent(ContainerService.KEY_CONTEXT).getSharedPreferences();

        String masterIDStr = prefs.getString(PREF_MASTER_ID, null);
        if (masterIDStr != null) {
            masterID = new DeviceID(masterIDStr);
            masterCert = getCertificate(masterID);
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
