package de.unipassau.isl.evs.ssh.drivers.lib;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;

/**
 * Class to get weather information from the Odroid Show and Weather board.
 *
 * @author Wolfram Gottschlich
 * @author Niko Fink
 * @version 2.0
 */
public class WeatherSensor extends AbstractComponent {
    public static final Key<WeatherSensor> KEY = new Key<>(WeatherSensor.class);
    private static final String TAG = WeatherSensor.class.getSimpleName();

    static {
        System.loadLibrary("ssh-drivers");
    }

    private double temp1, temp2, pressure, altitude, humidity, uv;
    private int visible, ir;

    @Override
    public void init(Container container) {
        initSerialInterface();
    }

    @Override
    public void destroy() {
        close();
    }

    public void updateData() {
        readData();
    }

    //Natives///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Initializes serial connection to Odroid Show and Weather board.
     * Remember to call close() at the end to close the connection.
     */
    private native void initSerialInterface();

    /**
     * Reads the current sensor data. The weather board only supports to read
     * all sensors together, it is not possible to read only a specific sensor.
     * Recorded values are also being logged in the Android log.
     */
    private native void readData();

    /**
     * closes the connection to the Odroid Show and Weather board via serial interface.
     */
    private native void close();

    //Getters///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
    * returns last value from readData() for temperature sensor 1
    */
    public double getTemperature1() {
        return temp1;
    }

    /*
    * returns last value from readData() for temperature sensor 2
    */
    public double getTemperature2() {
        return temp2;
    }

    /*
    * returns last value from readData() for pressure sensor
    */
    public double getPressure() {
        return pressure;
    }

    /*
    * returns last value from readData() for humidity sensor
    */
    public double getHumidity() {
        return humidity;
    }

    /*
    * returns last value from readData() for altitude sensor
    */
    public double getAltitude() {
        return altitude;
    }

    /*
    * returns last value from readData() for UV index sensor
    */
    public double getUV() {
        return uv;
    }

    /*
    * returns last value from readData() for visible light sensor
    */
    public int getVisibleLight() {
        return visible;
    }

    /*
    * returns last value from readData() for infrared sensor
    */
    public int getInfrared() {
        return ir;
    }
}
