package de.unipassau.isl.evs.ssh.drivers.lib;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
import io.netty.util.concurrent.Future;

/**
 * Class to get the state of the door buzzer actuator
 *
 * @author Wolfram Gottschlich
 * @version 0.1
 */
public class DoorBuzzer extends AbstractComponent {
    private final int address;

    /**
     * Constructor of the class representing the door buzzer actuator
     *
     * @param pin where the door buzzer is connected to the odroid
     */
    public DoorBuzzer(int pin) throws EvsIoException {
        address = pin;
        EvsIo.registerPin(pin, "out");
        EvsIo.setValue(address, false);
    }

    /**
     * Stops the door buzzer
     */
    public void lock() throws EvsIoException {
        EvsIo.setValue(address, false);
    }

    /**
     * Activates the door buzzer for the given time in milliseconds.
     * This method doesn't throw an EvsIoException, but returns a failed Future instead.
     *
     * @param ms time in milliseconds for which the door is unlocked
     */
    public Future<Void> unlock(int ms) {
        ExecutionServiceComponent exec = requireComponent(ExecutionServiceComponent.KEY);
        try {
            EvsIo.setValue(address, true);
        } catch (EvsIoException e) {
            return exec.newFailedFuture(e);
        }
        return exec.schedule(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                EvsIo.setValue(address, false);
                return null;
            }
        }, ms, TimeUnit.MILLISECONDS);
    }
}
