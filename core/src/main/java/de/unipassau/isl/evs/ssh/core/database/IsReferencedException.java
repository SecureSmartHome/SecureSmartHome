package de.unipassau.isl.evs.ssh.core.database;

/**
 * Exception should be used if the item that should be deleted is referenced by another item in the database.
 * E.g. if a Group should be deleted but there are still users in this Group.
 *
 * @author Leon Sell
 */
public class IsReferencedException extends DatabaseControllerException {
    public IsReferencedException() {
    }

    public IsReferencedException(String detailMessage) {
        super(detailMessage);
    }

    public IsReferencedException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public IsReferencedException(Throwable throwable) {
        super(throwable);
    }
}