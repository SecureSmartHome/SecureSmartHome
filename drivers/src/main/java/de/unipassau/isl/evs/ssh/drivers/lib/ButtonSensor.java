package de.unipassau.isl.evs.ssh.drivers.lib;

/**
 * Class to get the state of the push button
 *
 * @author Wolfram Gottschlich
 * @version 0.1
 */

public class ButtonSensor {
    int address;
    int dummyCount;

    /**
     * Constructor of the class representing a push button
     *
     * @param address where the button is connected to the odroid
     */
    public ButtonSensor(int IoAdress) {
        address = IoAdress;
        dummyCount = 0;
    }

    /**
     * Checks if the push button is currently pressed
     *
     * @return true if the push button is currently pressed
     */
    public boolean isPressed() throws EvsIoException {
        boolean ret = false;
        if (dummyCount < 5) {
            ret = false;
            dummyCount++;
        } else {
            ret = true;
            dummyCount = 0;
        }
        return ret;
    }

}
