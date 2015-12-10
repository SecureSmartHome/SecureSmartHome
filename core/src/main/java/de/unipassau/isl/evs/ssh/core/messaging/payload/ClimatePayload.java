package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;

/**
 * Payload class for WeatherSensor data.
 *
 * @author bucher
 */
public class ClimatePayload implements MessagePayload {

    private double temp1, temp2, pressure, altitude, humidity, uv;
    private int visible, ir;
    private String notificationType;
    private Module module;

    public ClimatePayload(double temp1, double temp2, double pressure, double altitude, double humidity,
                          double uv, int visible, int ir, String notificationType, Module module) {
        this.temp1 = temp1;
        this.temp2 = temp2;
        this.pressure = pressure;
        this.altitude = altitude;
        this.humidity = humidity;
        this.uv = uv;
        this.visible = visible;
        this.ir = ir;
        this.notificationType = notificationType; //see CoreConstants.NotificationTyp
        this.module = module;
    }

    /**
     * Creates a new payload based on a given one, ignoring the notification type of the old payload and allowing
     * to set a new one.
     *
     * @param payload          which is to be copied
     * @param notificationType that is to be set
     */
    public ClimatePayload(ClimatePayload payload, String notificationType) {
        this.module = payload.module;
        this.temp1 = payload.temp1;
        this.temp2 = payload.temp2;
        this.pressure = payload.pressure;
        this.altitude = payload.altitude;
        this.humidity = payload.humidity;
        this.uv = payload.uv;
        this.visible = payload.visible;
        this.ir = payload.ir;
        this.notificationType = notificationType;
    }

    /**
     * TODO write text
     *
     * @return ClimatePayload containing the ClimatePayload data.
     */
    public ClimatePayload ClimatePayload() {
        return ClimatePayload.this;
    }

    public Module getModule() { return module; }

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

    public Module getModuleName() {
        return module;
    }

    /**
     * Depending on the notification type a client decides if a warning shall be shown or not.
     *
     * @return String allowing the user to decide if a warning shall be displayed
     */
    public String getNotificationType() {
        return notificationType;
    }
}
