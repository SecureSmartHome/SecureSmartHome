package de.unipassau.isl.evs.ssh.drivers.lib;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class EvsIo {
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
        try (FileOutputStream os = new FileOutputStream(getValueFile(pin))) {
            os.write(value ? 1 : 0);
        } catch (IOException e) {
            throw new EvsIoException("Could not set value of pin " + pin + " to " + value, e);
        }
    }

    @NonNull
    private static File getValueFile(int pin) {
        return new File("/sys/class/gpio/gpio" + pin + "/value");
    }
}
