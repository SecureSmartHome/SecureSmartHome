package de.unipassau.isl.evs.ssh.master.database;

/**
 * Exception should be used if the given reference is not present in the database. E.g. adding a module with atSlave 12
 * when there's no Slave with id 12.
 *
 * @author Leon Sell
 */
public class UnknownReferenceException extends DatabaseControllerException {
    public UnknownReferenceException() {
    }

    public UnknownReferenceException(String detailMessage) {
        super(detailMessage);
    }

    public UnknownReferenceException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public UnknownReferenceException(Throwable throwable) {
        super(throwable);
    }
}
