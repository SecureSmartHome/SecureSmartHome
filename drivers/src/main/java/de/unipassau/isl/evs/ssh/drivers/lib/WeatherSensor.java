package de.unipassau.isl.evs.ssh.drivers.lib;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;

/**
 * Class to get weather information from the Odroid Show and Weather board.
 *
 * @author Wolfram Gottschlich
 * @author Niko Fink
 * @author Christoph Fraedrich
 * @version 2.1
 */
@SuppressWarnings("CanBeFinal")
public class WeatherSensor extends AbstractComponent {
    static {
        System.loadLibrary("ssh-drivers");
    }

    private double temp1, temp2, pressure, altitude, humidity, uv;
    private int visible, ir;
    private Module module;

    private ScheduledFuture future;

    @Override
    public void init(Container container) {
        super.init(container);
        initSerialInterface();
        this.future = container.require(ExecutionServiceComponent.KEY).scheduleAtFixedRate(
                new WeatherPollingRunnable(), 0, 200, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        close();
        future.cancel(true);
        super.destroy();
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

    /**
     * @return the last value from readData() for temperature sensor 1
     */
    public double getTemperature1() {
        return temp1;
    }

    /**
     * @return the last value from readData() for temperature sensor 2
     */
    public double getTemperature2() {
        return temp2;
    }

    /**
     * @return the last value from readData() for pressure sensor
     */
    public double getPressure() {
        return pressure;
    }

    /**
     * @return the last value from readData() for humidity sensor
     */
    public double getHumidity() {
        return humidity;
    }

    /**
     * @return the last value from readData() for altitude sensor
     */
    public double getAltitude() {
        return altitude;
    }

    /**
     * @return the last value from readData() for UV index sensor
     */
    public double getUV() {
        return uv;
    }

    /**
     * @return the last value from readData() for visible light sensor
     */
    public int getVisibleLight() {
        return visible;
    }

    /**
     * @return the last value from readData() for infrared sensor
     */
    public int getInfrared() {
        return ir;
    }

    public Module getModule() {
        return module; //FIXME always returns null (Niko, 2016-01-03)
    }

    private class WeatherPollingRunnable implements Runnable {
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
            ClimatePayload payload = new ClimatePayload(getTemperature1(), getTemperature2(), getPressure(),
                    getAltitude(), getHumidity(), getUV(), getVisibleLight(), getInfrared(), "", getModule());

            NamingManager namingManager = requireComponent(NamingManager.KEY);

            Message message = new Message(payload);
            OutgoingRouter router = requireComponent(OutgoingRouter.KEY);
            router.sendMessage(namingManager.getMasterID(), RoutingKeys.MASTER_PUSH_WEATHER_INFO, message);
        }
    }
}
