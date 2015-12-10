package de.unipassau.isl.evs.ssh.core.network.handshake;

import java.security.GeneralSecurityException;

/**
 * @author Niko Fink
 */
public class HandshakeException extends GeneralSecurityException {
    public HandshakeException() {
    }

    public HandshakeException(String detailMessage) {
        super(detailMessage);
    }

    public HandshakeException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public HandshakeException(Throwable throwable) {
        super(throwable);
    }
}
