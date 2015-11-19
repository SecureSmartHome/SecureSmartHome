package de.unipassau.isl.evs.ssh.master.database;

/**
 * Exception should be used if the given reference is not present in the database. E.g. adding a module with atSlave 12
 * when there's no Slave with id 12.
 */
public class IllegalReferenceException extends DatabaseControllerException {
    public IllegalReferenceException() {
    }

    public IllegalReferenceException(String detailMessage) {
        super(detailMessage);
    }

    public IllegalReferenceException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public IllegalReferenceException(Throwable throwable) {
        super(throwable);
    }
}
