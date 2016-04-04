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

package de.unipassau.isl.evs.ssh.master;

/**
 * This Constants class provides constants needed by the master module.
 *
 * @author Team
 */
public enum MasterConstants {
    ;

    /**
     * Containing thresholds for climate data. If thresholds are bypassed a warning should be sent.
     *
     * @author Christoph Fraedrich
     */
    public enum ClimateThreshold {
        ;

        public static final int ALTITUDE = 100;
        //Threshold Humidity 80%. Mold will start to grow at this value.
        public static final double HUMIDITY = 80;
        //Normal air pressure at sea level.
        public static final double PRESSURE = 101325;
        public static final double TEMP1 = 21;
        public static final double TEMP2 = 25;
        //Bright Day 100 Lux. Cloudy Day 20 Lux. -> Threshold 60 Lux
        public static final int VISIBLE_LIGHT = 60;
    }
}
