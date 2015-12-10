package de.unipassau.isl.evs.ssh.drivers.lib;

import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;

/**
 * Class to get the state of the door buzzer actuator
 *
 * @author Wolfram Gottschlich
 * @version 0.1
 */
public class DoorBuzzer extends AbstractComponent {
    int address;

    /**
     * Constructor of the class representing the door buzzer actuator
     *
     * @param IoAddress where the door buzzer is connected to the odroid
     */
    public DoorBuzzer(int IoAddress) throws EvsIoException {
        address = IoAddress;
        EvsIo.registerPin(IoAddress, "out");
        EvsIo.setValue(address, false);
    }

    /**
     * Looks the door
     */
    public void lock() throws EvsIoException {
        EvsIo.setValue(address, false);
    }

    /**
     * Looks the door
     *
     * @param ms time in milli seconds for which the door is unlocked
     */
    public void unlock(int ms) throws EvsIoException {
        EvsIo.setValue(address, true);
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        EvsIo.setValue(address, false);

    }

}
