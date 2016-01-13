package de.unipassau.isl.evs.ssh.core.database;

/**
 * Exception combines any amount of exceptions of subclasses of the DatabaseControllerException.
 *
 * @author Leon Sell
 */
public class DatabaseControllerException extends Exception {
    public DatabaseControllerException() {
    }

    public DatabaseControllerException(String detailMessage) {
        super(detailMessage);
    }

    public DatabaseControllerException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public DatabaseControllerException(Throwable throwable) {
        super(throwable);
    }
}
