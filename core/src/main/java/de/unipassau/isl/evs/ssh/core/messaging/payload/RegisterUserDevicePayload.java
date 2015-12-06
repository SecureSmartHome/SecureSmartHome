package de.unipassau.isl.evs.ssh.core.messaging.payload;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.X509Certificate;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

public class RegisterUserDevicePayload implements MessagePayload {
    byte[] token;
    DeviceID userDeviceID;
    //certificate will be null when used for initRegistration!
    X509Certificate certificate;

    public RegisterUserDevicePayload(byte[] token, X509Certificate certificate) throws NoSuchProviderException, NoSuchAlgorithmException {
        this.token = token;
        userDeviceID = DeviceID.fromCertificate(certificate);
        this.certificate = certificate;
    }

    public RegisterUserDevicePayload(byte[] token, DeviceID userDeviceID) {
        this.token = token;
        this.userDeviceID = userDeviceID;
    }

    public byte[] getToken() {
        return token;
    }

    public void setToken(byte[] token) {
        this.token = token;
    }

    public DeviceID getUserDeviceID() {
        return userDeviceID;
    }

    public void setUserDeviceID(DeviceID userDeviceID) {
        this.userDeviceID = userDeviceID;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }
}
