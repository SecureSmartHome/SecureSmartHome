package de.unipassau.isl.evs.ssh.master.database;

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
