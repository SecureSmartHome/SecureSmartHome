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

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Utility class for getting and setting GPIO pins.
 *
 * @author Betreuer
 * @author Niko Fink
 */
public enum EvsIo {
    ;
    private static final String TAG = EvsIo.class.getSimpleName();

    /**
     * Set-Up a GPIO pin
     *
     * @param pin       pin number (see shifter shield)
     * @param direction for sensors "in" and for actuators "out"
     */
    public static void registerPin(int pin, String direction) throws EvsIoException {
        try {
            Log.d(TAG, "Register GPIO " + pin + " with direction " + direction);
            final Process su = Runtime.getRuntime().exec("su");
            final PrintWriter w = new PrintWriter(su.getOutputStream());

            w.println("echo " + pin + " > /sys/class/gpio/export");
            w.println("echo \"" + direction + "\" > /sys/class/gpio/gpio" + pin + "/direction");
            ////edge detection for sensors: rising/falling/both/none
            //if (Strings.isNullOrEmpty(edge)) {
            //    edge = "none";
            //}
            //w.println("echo \"" + edge + "\" > /sys/class/gpio/gpio" + pin + "/edge");
            w.println("chmod 666 " + getValueFile(pin));
            w.flush();

            w.println("exit");
            w.flush();
            su.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new EvsIoException("Could not register pin " + pin, e);
        }
    }

    /**
     * Read sensor value
     *
     * @param pin pin number (see shifter shield)
     * @return the value of the sensor
     */
    public static String readValue(int pin) throws EvsIoException {
        try {
            return Files.toString(getValueFile(pin), Charsets.UTF_8);
        } catch (IOException e) {
            throw new EvsIoException("Could not read value of pin " + pin, e);
        }
    }

    /**
     * Set actuator state
     *
     * @param pin   pin number (see shifter shield)
     * @param value value to set
     */
    public static void setValue(int pin, boolean value) throws EvsIoException {
        try (FileWriter os = new FileWriter(getValueFile(pin))) {
            os.write(value ? "1\n" : "0\n");
        } catch (IOException e) {
            throw new EvsIoException("Could not set value of pin " + pin + " to " + value, e);
        }
    }

    @NonNull
    private static File getValueFile(int pin) {
        return new File("/sys/class/gpio/gpio" + pin + "/value");
    }
}
