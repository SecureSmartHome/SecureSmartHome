/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
