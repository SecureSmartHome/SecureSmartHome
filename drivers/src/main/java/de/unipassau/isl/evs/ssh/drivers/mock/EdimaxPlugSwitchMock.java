package de.unipassau.isl.evs.ssh.drivers.mock;

import java.io.IOException;

import de.unipassau.isl.evs.ssh.drivers.lib.EdimaxPlugSwitch;

/**
 * @author Niko Fink
 */
public class EdimaxPlugSwitchMock extends EdimaxPlugSwitch {
    public EdimaxPlugSwitchMock() {
        super("127.0.0.1");
    }

    private boolean isOn = false;

    @Override
    public boolean isOn() throws IOException {
        return isOn;
    }

    @Override
    public boolean setOn(boolean on) throws IOException {
        isOn = on;
        return true;
    }
}
