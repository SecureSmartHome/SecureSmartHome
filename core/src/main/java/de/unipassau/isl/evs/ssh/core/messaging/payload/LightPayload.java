package de.unipassau.isl.evs.ssh.core.messaging.payload;

/**
 * Payload class for Light Messages
 *
 * @Author Chris
 */
public class LightPayload implements MessagePayload {

    private boolean on;

    //TODO change constructors so only a boolean is contained once we have the component which handles address stuff
    public LightPayload(boolean on) {
        this.on = on;
    }

    /**
     * Returns a boolean indicating whether the light should switched on.
     * If not it means the opposite, that is, the light should be switched off, not just "not switched on".
     *
     * @return true if the light should be switched on, false if the light should be switched off.
     */
    public boolean getOn() {
        return on;
    }
}
