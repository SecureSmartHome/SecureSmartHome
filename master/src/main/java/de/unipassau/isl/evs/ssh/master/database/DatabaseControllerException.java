package de.unipassau.isl.evs.ssh.master.database;

public abstract class DatabaseControllerException extends Exception {
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
