package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;

/**
 * Payload class for WeatherSensor data.
 *
 * @author Andreas Bucher
 */
public class ClimatePayload implements MessagePayload {

    private final double temp1;
    private final double temp2;
    private final double pressure;
    private final double altitude;
    private final double humidity;
    private final double uv;
    private final int visible;
    private final int ir;
    private final Module module;

    /**
     * Constructor for a ClimatePayload, containing current data from the WeatherBoard.
     *
     * @param temp1    Temperature 1
     * @param temp2    Temperature 2
     * @param pressure Air pressure
     * @param altitude Altitude
     * @param humidity Humidity
     * @param uv       UV Index
     * @param visible  Visible light in Lux
     * @param ir       IR light in Lux
     * @param module   The WeatherBoard containing this data
     */
    public ClimatePayload(double temp1, double temp2, double pressure, double altitude, double humidity,
                          double uv, int visible, int ir, Module module) {
        this.temp1 = temp1;
        this.temp2 = temp2;
        this.pressure = pressure;
        this.altitude = altitude;
        this.humidity = humidity;
        this.uv = uv;
        this.visible = visible;
        this.ir = ir;
        this.module = module;
    }

    public Module getModule() {
        return module;
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

    public Module getModuleName() {
        return module;
    }
}
