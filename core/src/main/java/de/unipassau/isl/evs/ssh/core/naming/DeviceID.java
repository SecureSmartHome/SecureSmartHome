package de.unipassau.isl.evs.ssh.core.naming;

/**
 * Unique id for all devices (user devices, master, slaves).
 */
public class DeviceID {
    private String fingerprint;

    public DeviceID() {
    }

    public DeviceID(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }
}