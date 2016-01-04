package de.unipassau.isl.evs.ssh.core.network;

/**
 * TODO JavaDoc
 * @author Niko Fink
 */
public interface ClientConnectionListener {
    void onMasterFound();

    void onClientConnecting(String host, int port);

    void onClientConnected();

    void onClientDisconnected();

    void onClientRejected(String message);
}
