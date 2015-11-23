package de.unipassau.isl.evs.ssh.drivers.lib;

import android.util.Log;

public class Weather {
    private static final String TAG = "Odroid Weather";

    static {
        System.loadLibrary("odroid_weather");
    }

    private double temp1, temp2, pressure, altitude, humidity, uv;

    //    @Override
    //    public void onCreate(Bundle savedInstanceState) {
    //        super.onCreate(savedInstanceState);
    //        this.weatherBoardTest();
    //    }
    private int visible, ir;

    /*
    * Initializes serial connection to Odroid Show and Weather board.
    * Remember to call close() at the end to close the connection.
    */
    public native void initSerialInterface();

    /*
    * Reads the current sensor data. The weather board only supports to read
    * all sensors together, it is not possible to read only a specific sensor.
    * Recorded values are also being logged in the Android log.
    */
    public native void readData();

    /*
    * closes the connection to the Odroid Show and Weather board via serial interface.
    */
    public native void close();

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

    public void weatherBoardTest() {
        Log.d(TAG, "testing weather board");
        initSerialInterface();
        readData();
        Log.d(TAG, "temperature1 = " + this.getTemperature1());
        Log.d(TAG, "Now closing file descriptor.");
        close();
        Log.d(TAG, "finished!");
    }
}
