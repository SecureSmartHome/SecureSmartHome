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

package de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint;

/**
 * ModuleAccessPoint class for a GPIO access point.
 *
 * @author Leon Sell
 */
public class GPIOAccessPoint extends ModuleAccessPoint {
    public static final String TYPE = "GPIO";
    private int port;

    public GPIOAccessPoint(int port) {
        this.port = port;
    }

    /**
     * Create GPIOAccessPoint from combined access point information.
     *
     * @param combinedModuleAccessPointInformation The combined access point information.
     * @return The constructed GPIOAccessPoint.
     */
    public static GPIOAccessPoint fromCombinedModuleAccessPointInformation(
            String[] combinedModuleAccessPointInformation) {
        return new GPIOAccessPoint(Integer.valueOf(combinedModuleAccessPointInformation[0]));
    }

    @Override
    public String[] getAccessInformation() {
        return new String[]{String.valueOf(port)};
    }

    @Override
    public int[] getDatabaseIndices() {
        return new int[]{0};
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return getType() + "#" + getPort();
    }
}
