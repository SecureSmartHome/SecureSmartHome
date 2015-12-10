package de.unipassau.isl.evs.ssh.drivers.lib;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by wg on 07.12.15.
 */
public class EvsIo {

    /*
     * Initialisation of a pin
     *
     * @param IoAddress Pin number (see shifter shild)
	 * @param direction For sensors "in" and for actuators "out"
	 *
	 * @return True if OK
     */
    public static boolean registerPin(int IoAddress, String direction) throws EvsIoException {
        boolean ret = true;

        // Register sensor
        try {
            Log.w("EVS-IO", "EVS-IO: Register GPIO " + IoAddress + " with direction: " + direction);
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            outputStream.writeBytes("echo " + IoAddress + " > /sys/class/gpio/export\n");
            outputStream.writeBytes("echo \"" + direction + "\" > /sys/class/gpio/gpio" + IoAddress + "/direction\n");
            outputStream.flush();

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            su.waitFor();
        } catch (Exception e) {
            Log.w("EVS-IO", "EVS-IO error: " + e);
            throw new EvsIoException("Could not register Pin: " + e);
        }
        return ret;
    }

    /*
     * Read sensor value
     *
     * @param IoAddress Pin number (see shifter shild)
     *
     * @return The read value
    */
    public static String readValue(int IoAdress) throws EvsIoException {
        String ret = "";

        // Register sensor
        try {
            InputStream response = null;
            Log.w("EVS-IO", "EVS-IO: Read GPIO " + IoAdress);
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
            response = su.getInputStream();


            outputStream.writeBytes("cat /sys/class/gpio/gpio" + IoAdress + "/value\n");
            outputStream.flush();

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            su.waitFor();
            String result = readFully(response);
            Log.w("EVS-IO", "EVS-IO: Read GPIO value: " + result);
            ret = result;
        } catch (Exception e) {
            Log.w("EVS-IO", "EVS-IO error: " + e);
            throw new EvsIoException("Could not register Pin: " + e);
        }
        return ret;
    }

    /*
     * Set actuator state
     *
     * @param IoAddress Pin number (see shifter shild)
     * @param value value to set
     *
     * @return True if OK
    */
    public static boolean setValue(int IoAdress, boolean value) throws EvsIoException {
        boolean ret = false;

        // Register sensor
        try {
            Log.w("EVS-IO", "EVS-IO: Set GPIO " + IoAdress);
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
            int valueInt = 0;

            if (value == true) {
                valueInt = 1;
            } else {
                valueInt = 0;
            }

            outputStream.writeBytes("echo " + valueInt + " > /sys/class/gpio/gpio" + IoAdress + "/value\n");
            outputStream.flush();

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            su.waitFor();
            ret = true;
        } catch (Exception e) {
            Log.w("EVS-IO", "EVS-IO error: " + e);
            throw new EvsIoException("Could not register Pin: " + e);
        }
        return ret;
    }

    public static String readFully(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = is.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toString("UTF-8");
    }

}
