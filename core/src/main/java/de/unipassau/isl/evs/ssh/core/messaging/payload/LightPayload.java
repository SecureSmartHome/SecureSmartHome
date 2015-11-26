package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;

/**
 * Payload class for Light Messages
 *
 * @Author Chris
 */
public class LightPayload implements MessagePayload {

    private Module module;
    private boolean on;

    //TODO change constructors so only a boolean is contained once we have the component which handles address stuff
    public LightPayload(boolean on, Module module) {
        this.on = on;
        this.module = module;
    }

    /**
     * Returns a boolean indicating whether the light should switched on or is on.
     * If not it means the opposite, that is, the light should be switched off, not just "not switched on".
     *
     * Whether the light status is only checked or switched depends on the used routing key.
     *
     * @return true if the light should be switched on, false if the light should be switched off.
     */
    public boolean getOn() {
        return on;
    }

    /**
     * Returns a String indicating which lamped should be switched or checked.
     *
     * @return String indicating the module name
     */
    public Module getModule() {
        return module;
    }
}
