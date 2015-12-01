package de.unipassau.isl.evs.ssh.core.messaging.payload;

/**
 * Payload class for messages regarding door bell events
 *
 * @author Chris
 */
public class DoorBellPayload implements MessagePayload {

    String moduleName; //Name of the bell which is rang

    public DoorBellPayload(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getModuleName() {
        return moduleName;
    }
}
