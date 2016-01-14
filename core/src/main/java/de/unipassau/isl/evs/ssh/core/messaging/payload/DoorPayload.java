package de.unipassau.isl.evs.ssh.core.messaging.payload;

/**
 * @author Leon Sell
 */
public class DoorPayload implements MessagePayload {
    private final String moduleName;

    public DoorPayload(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getModuleName() {
        return moduleName;
    }
}
