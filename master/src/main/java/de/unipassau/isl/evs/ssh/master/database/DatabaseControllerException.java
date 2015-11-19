package de.unipassau.isl.evs.ssh.master.database;

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
