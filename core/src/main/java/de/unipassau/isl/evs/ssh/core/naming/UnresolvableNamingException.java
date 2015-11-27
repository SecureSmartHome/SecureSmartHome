package de.unipassau.isl.evs.ssh.core.naming;

/**
 * Thrown when a naming mapping cannot be resolved to a DeviceID or public key.
 * @author Wolfgang Popp
 */
public class UnresolvableNamingException extends Exception {

    public UnresolvableNamingException() {
    }

    public UnresolvableNamingException(String detailMessage) {
        super(detailMessage);
    }

    public UnresolvableNamingException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public UnresolvableNamingException(Throwable throwable) {
        super(throwable);
    }

}
