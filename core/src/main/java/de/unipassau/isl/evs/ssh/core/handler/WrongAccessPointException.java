package de.unipassau.isl.evs.ssh.core.handler;

/**
 * @author Wolfgang Popp.
 */
public class WrongAccessPointException extends Exception {
    public WrongAccessPointException() {
        super();
    }

    public WrongAccessPointException(String message) {
        super(message);
    }

    public WrongAccessPointException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongAccessPointException(Throwable cause) {
        super(cause);
    }
}
