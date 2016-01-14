package de.unipassau.isl.evs.ssh.core.naming;

/**
 * Thrown when a naming mapping cannot be resolved to a DeviceID or public key.
 *
 * @author Wolfgang Popp
 */
public class UnresolvableNamingException extends Exception {

    /**
     * Constructs a new UnresolvableNamingException with a null detail message.
     */
    public UnresolvableNamingException() {
        super();
    }

    /**
     * Constructs a new UnresolvableNamingException with the given detail message.
     *
     * @param detailMessage the detailed message
     */
    public UnresolvableNamingException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Constructs a new UnresolvableNamingException with the given detail message and cause.
     *
     * @param detailMessage the detailed message
     * @param throwable     the throwable which caused this exception
     */
    public UnresolvableNamingException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    /**
     * Constructs a new UnresolvableNamingException with the given cause.
     *
     * @param throwable the throwable which caused this exception
     */
    public UnresolvableNamingException(Throwable throwable) {
        super(throwable);
    }

}
