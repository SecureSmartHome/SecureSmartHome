package de.unipassau.isl.evs.ssh.core.handler;

/**
 * Thrown when a module is configured with the wrong access point type.
 *
 * @author Wolfgang Popp
 */
public class WrongAccessPointException extends Exception {

    /**
     * Constructs a new WrongAccessPointException without a detail message.
     */
    public WrongAccessPointException() {
        super();
    }

    /**
     * Constructs a new WrongAccessPointException with the given detail message.
     *
     * @param message the detail message
     */
    public WrongAccessPointException(String message) {
        super(message);
    }
}
