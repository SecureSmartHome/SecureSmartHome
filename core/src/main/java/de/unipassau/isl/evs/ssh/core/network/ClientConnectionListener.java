package de.unipassau.isl.evs.ssh.core.network;

/**
 * @author Niko Fink
 */
public interface ClientConnectionListener {
    void onMasterFound();

    void onClientConnecting();

    void onClientConnected();

    void onClientDisconnected();

    void onClientRejected(String message);
}
