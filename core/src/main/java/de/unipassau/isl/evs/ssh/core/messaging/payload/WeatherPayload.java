package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.CoreConstants;

/**
 * Payload class for WeatherSensor data.
 *
 * @author bucher
 */
public class WeatherPayload implements MessagePayload {

    private double temp1, temp2, pressure, altitude, humidity, uv;
    private int visible, ir;
    private String notificationType, modulName;

    public WeatherPayload(double temp1, double temp2, double pressure, double altitude, double humidity,
                          double uv, int visible, int ir, String notificationType, String modulName ) {
        this.temp1 = temp1;
        this.temp2 = temp2;
        this.pressure = pressure;
        this.altitude = altitude;
        this.humidity = humidity;
        this.uv = uv;
        this.visible = visible;
        this.ir = ir;
        this.notificationType = notificationType; //see CoreConstants.NotificationTypd
        this.modulName = modulName;
    }

    /**
     * Creates a new payload based on a given one, ignoring the notification type of the old payload and allowing
     * to set a new one.
     *  @param payload which is to be copied
     * @param notificationType that is to be set
     */
    public WeatherPayload(WeatherPayload payload, String notificationType) {
        this.temp1 = payload.temp1;
        this.temp2 = payload.temp2;
        this.pressure = payload.pressure;
        this.altitude = payload.altitude;
        this.humidity = payload.humidity;
        this.uv = payload.uv;
        this.visible = payload.visible;
        this.ir = payload.ir;
        this.notificationType = notificationType;
        this.modulName = modulName;
    }

    /**
     * TODO write text
     *
     * @return WeatherPayload containing the WeatherSensor data.
     */
    public WeatherPayload getWeatherData() {
        return WeatherPayload.this;
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

    public String getModulName() { return modulName; }

    /**
     * Depending on the notification type a client decides if a warning shall be shown or not.
     *
     * @return String allowing the user to decide if a warning shall be displayed
     */
    public String getNotificationType() {
        return notificationType;
    }
}
