/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
