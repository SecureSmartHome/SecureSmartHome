package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;

/**
 * Payload class for message to add new Sensor to the System.
 *
 * @author Andreas Bucher
 */
public class ModifyModulePayload implements MessagePayload {

    private final Module module;

    public ModifyModulePayload(Module module) {
        this.module = module;
    }

    public Module getModule() {
        return module;
    }
}
