package de.unipassau.isl.evs.ssh.core.messaging.payload;

/**
 * @author leon
 */
public class DoorPayload implements MessagePayload {
    boolean unlock;

    public DoorPayload(boolean unlock) {
        this.unlock = unlock;
    }

    public boolean isUnlock() {
        return unlock;
    }

    public void setUnlock(boolean unlock) {
        this.unlock = unlock;
    }
}
