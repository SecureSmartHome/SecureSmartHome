package de.unipassau.isl.evs.ssh.core.messaging.payload;

/**
 * @author Leon Sell
 */
public class DoorLockPayload implements MessagePayload {
    boolean unlock;
    String moduleName;

    public DoorLockPayload(boolean unlock, String moduleName) {
        this.unlock = unlock;
        this.moduleName = moduleName;
    }

    public boolean isUnlock() {
        return unlock;
    }

    public void setUnlock(boolean unlock) {
        this.unlock = unlock;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }
}
