package de.unipassau.isl.evs.ssh.drivers.lib;

import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;

/**
 * Class to get the values form a window/door sensor
 *
 * @author Wolfram Gottschlich
 * @version 0.1
 */

public class ReedSensor extends AbstractComponent {
    int address;
    int dummyCount;

    /**
     * Constructor of the class representing door and window sensors
     *
     * @param address where the sensor is connected to the odroid
     */
    public ReedSensor(int IoAdress) {
        address = IoAdress;
        dummyCount = 0;
    }

    /**
     * Checks if the window is open
     *
     * @return true if the window is currently open
     */
    public boolean isOpen() throws EvsIoException {
        boolean ret = true;
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
