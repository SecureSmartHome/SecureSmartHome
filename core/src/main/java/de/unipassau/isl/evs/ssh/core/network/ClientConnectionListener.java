package de.unipassau.isl.evs.ssh.core.network;

import android.support.annotation.Nullable;

/**
 * A Listener that can be notified when the Connected State of the {@link Client} changes.
 *
 * @author Niko Fink
 */
public interface ClientConnectionListener {
    /**
     * Called when a new possible address for the Master was found, e.g. by UDP discovery.
     */
    void onMasterFound();

    /**
     * Called when the Client is attempting to establish a connection with the Master listening at the given address.
     */
    void onClientConnecting(String host, int port);

    /**
     * Called as soon as the connection is established and the Client is authenticated and
     * {@link Client#isConnectionEstablished()} will return {@code true}.
     */
    void onClientConnected();

    /**
     * Called as soon as the connection broke and
     * {@link Client#isConnectionEstablished()} will return {@code false}.
     */
    void onClientDisconnected();

    /**
     * Called when the Master rejected this Client with a given message.
     */
    void onClientRejected(@Nullable String message);
}
