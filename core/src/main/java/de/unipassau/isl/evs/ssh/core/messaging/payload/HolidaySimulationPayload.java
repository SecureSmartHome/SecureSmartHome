package de.unipassau.isl.evs.ssh.core.messaging.payload;

/**
 * Payload class for messages to switch the holiday simulation on or off
 *
 * @author Christoph Fraedrich
 */
public class HolidaySimulationPayload implements MessagePayload {

    private final boolean on; //either indicates if the simulation should be switch on or off, or if it is switched on or off

    /**
     * Constructor for the payload
     *
     * @param on showing whether the holiday simulation should be switched on or is on, depending
     *           on the use of the payload
     */
    public HolidaySimulationPayload(boolean on) {
        this.on = on;
    }

    public boolean switchOn() {
        return on;
    }
}
