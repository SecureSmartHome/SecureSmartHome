package de.unipassau.isl.evs.ssh.master.database;

public class InUseException extends DatabaseControllerException {
    public InUseException() {
    }

    public InUseException(String detailMessage) {
        super(detailMessage);
    }

    public InUseException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public InUseException(Throwable throwable) {
        super(throwable);
    }
}
