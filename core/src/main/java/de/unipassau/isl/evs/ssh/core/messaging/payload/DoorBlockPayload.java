package de.unipassau.isl.evs.ssh.core.messaging.payload;

/**
 * @author Leon Sell
 */
public class DoorBlockPayload extends DoorPayload {
    private boolean lock;

    public DoorBlockPayload(boolean lock, String moduleName) {
        super(moduleName);
        this.lock = lock;
    }

    public boolean isLock() {
        return lock;
    }
}
