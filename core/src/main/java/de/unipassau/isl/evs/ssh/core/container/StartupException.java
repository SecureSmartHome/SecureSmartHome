package de.unipassau.isl.evs.ssh.core.container;

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
