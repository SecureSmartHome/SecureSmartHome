package de.unipassau.isl.evs.ssh.drivers.lib;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.WeatherPayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Class to get weather information from the Odroid Show and Weather board.
 *
 * @author Wolfram Gottschlich
 * @author Niko Fink
 * @author Chris
 * @version 2.1
 */
public class WeatherSensor extends AbstractComponent {
    public static final Key<WeatherSensor> KEY = new Key<>(WeatherSensor.class);
    private static final String TAG = WeatherSensor.class.getSimpleName();

    static {
        System.loadLibrary("ssh-drivers");
    }

    private double temp1, temp2, pressure, altitude, humidity, uv;
    private int visible, ir;
    private Container container;
    private ScheduledFuture future;

    @Override
    public void init(Container container) {
        initSerialInterface();
        this.container = container;

        this.future =  container.require(ExecutionServiceComponent.KEY).scheduleAtFixedRate(
                new WeatherPollingRunnable(this), 0, 200, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        close();
        future.cancel(true);
    }

    public void updateData() {
        readData();
    }

    private class WeatherPollingRunnable implements Runnable {

        WeatherSensor sensor;

        public WeatherPollingRunnable(WeatherSensor sensor) {
            this.sensor = sensor;
        }

        @Override
        public void run() {
            if (future != null) {
                updateData();
                sendWeatherInfo();
            }
        }

        /**
         * Sends Weather Information to Master
         */
        private void sendWeatherInfo() {
            WeatherPayload payload = new WeatherPayload(getTemperature1(), getTemperature2(), getPressure(),
                    getAltitude(), getHumidity(), getUV(), getVisibleLight(), getInfrared(), "");

            //Todo set values

            NamingManager namingManager = container.require(NamingManager.KEY);

            Message message;
            message = new Message(payload);
            message.putHeader(Message.HEADER_TIMESTAMP, System.currentTimeMillis());

            OutgoingRouter router = container.require(OutgoingRouter.KEY);
            router.sendMessage(namingManager.getMasterID(), CoreConstants.RoutingKeys.MASTER_WEATHER_INFO, message);
        }
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
