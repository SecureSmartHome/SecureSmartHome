package de.unipassau.isl.evs.ssh.master.database;

/**
 * Exception should be used if a UNIQUE constraint is violated. E.g. adding a new Slave with a name that is already
 * used by another Slave.
 *
 * @author Leon Sell
 */
public class AlreadyInUseException extends DatabaseControllerException {
    public AlreadyInUseException() {
    }

    public AlreadyInUseException(String detailMessage) {
        super(detailMessage);
    }

    public AlreadyInUseException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public AlreadyInUseException(Throwable throwable) {
        super(throwable);
    }
}
