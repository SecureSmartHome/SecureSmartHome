package de.unipassau.isl.evs.ssh.master.database;

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
