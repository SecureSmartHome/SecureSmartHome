package de.unipassau.isl.evs.ssh.drivers.lib;

/**
 * Class representing a EVS sensor exception
 *
 * @author Wolfram Gottschlich
 * @version 1.0
 */
public class EvsIoException extends Exception {

    public EvsIoException() {
        super();
    }

    public EvsIoException(String message) {
        super(message);
    }

    public EvsIoException(String message, Throwable cause) {
        super(message, cause);
    }

    public EvsIoException(Throwable cause) {
        super(cause);
    }
}

