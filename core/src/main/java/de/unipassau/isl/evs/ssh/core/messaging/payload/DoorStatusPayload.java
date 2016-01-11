package de.unipassau.isl.evs.ssh.core.messaging.payload;

/**
 * @author Leon Sell
 */
public class DoorStatusPayload extends DoorPayload {
    final boolean isOpen;
    final boolean isBlocked;

    public DoorStatusPayload(boolean isOpen, boolean isBlocked, String moduleName) {
        super(moduleName);
        this.isOpen = isOpen;
        this.isBlocked = isBlocked;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean isBlocked() {
        return isBlocked;
    }
}
