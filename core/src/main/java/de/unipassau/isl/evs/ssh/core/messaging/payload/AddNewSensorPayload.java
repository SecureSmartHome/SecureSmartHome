package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;

/**
 * Payload class for message to add new Sensor to the System.
 *
 * @author bucher
 */
public class AddNewSensorPayload implements MessagePayload {

    private Module module;

    public AddNewSensorPayload(Module module) {
        this.module = module;
    }

    public Module getModule() {
        return module;
    }
}
