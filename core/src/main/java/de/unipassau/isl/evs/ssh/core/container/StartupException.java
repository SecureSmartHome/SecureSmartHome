package de.unipassau.isl.evs.ssh.core.container;

/**
 * Exception which is to be thrown if the startup of a Component fails.
 *
 * @author Niko Fink
 */
public class StartupException extends RuntimeException {
    public StartupException() {
    }

    public StartupException(String detailMessage) {
        super(detailMessage);
    }

    public StartupException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public StartupException(Throwable throwable) {
        super(throwable);
    }
}
