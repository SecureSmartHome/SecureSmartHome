package de.unipassau.isl.evs.ssh.core.messaging.payload;

/**
 * @author Leon Sell
 */
public class DoorStatusPayload implements MessagePayload {
    final boolean closed;
    final String moduleName;

    public DoorStatusPayload(boolean closed, String moduleName) {
        this.closed = closed;
        this.moduleName = moduleName;
    }

    public boolean isClosed() {
        return closed;
    }

    public String getModuleName() {
        return moduleName;
    }
}
