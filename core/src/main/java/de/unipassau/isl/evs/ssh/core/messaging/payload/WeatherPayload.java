package de.unipassau.isl.evs.ssh.core.messaging.payload;

/**
 * Payload class for WeatherSensor data.
 *
 * @author bucher
 */
public class WeatherPayload implements MessagePayload {

    private double temp1, temp2, pressure, altitude, humidity, uv;
    private int visible, ir;
    private boolean climate;

    public WeatherPayload(double temp1, double temp2, double pressure, double altitude, double humidity,
                          double uv, int visible, int ir, boolean climate) {
        this.temp1 = temp1;
        this.temp2 = temp2;
        this.pressure = pressure;
        this.altitude = altitude;
        this.humidity = humidity;
        this.uv = uv;
        this.visible = visible;
        this.ir = ir;
        this.climate = climate;
    }

    /**
     * TODO write text
     *
     * @return WeatherPayload containing the WeatherSensor data.
     */
    public WeatherPayload getWeatherData() {
        return WeatherPayload.this;
    }

    /**
     * If boolean climate == true, the User receives a notification to open his windows.
     * When false, the humidity in the room is within the limits.
     *
     * @return boolean indicating if the User should be notified
     */
    public boolean climateWarning() {
        return climate;
    }

    public double getTemp1() {
        return temp1;
    }

    public double getTemp2() {
        return temp2;
    }

    public double getPressure() {
        return pressure;
    }

    public double getAltitude() {
        return altitude;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getUv() {
        return uv;
    }

    public int getVisible() {
        return visible;
    }

    public int getIr() {
        return ir;
    }
}
