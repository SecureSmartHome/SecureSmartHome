package de.unipassau.isl.evs.ssh.core.naming;

/**
 * Created by popeye on 11/19/15.
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
