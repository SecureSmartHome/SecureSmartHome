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
 * ModuleAccessPoint class for a WLAN access point.
 *
 * @author Leon Sell
 */
public class WLANAccessPoint extends ModuleAccessPoint {
    public static final String TYPE = "WLAN";
    private int port;
    private String username;
    private String password;
    private String iPAddress;

    public WLANAccessPoint(int port, String username, String password, String iPAddress) {
        this.port = port;
        this.username = username;
        this.password = password;
        this.iPAddress = iPAddress;
    }

    /**
     * Create WLANAccessPoint from combined access point information.
     *
     * @param combinedModuleAccessPointInformation The combined access point information.
     * @return The constructed WLANAccessPoint.
     */
    public static WLANAccessPoint fromCombinedModuleAccessPointInformation(
            String[] combinedModuleAccessPointInformation) {
        return new WLANAccessPoint(Integer.valueOf(combinedModuleAccessPointInformation[2]),
                combinedModuleAccessPointInformation[3], combinedModuleAccessPointInformation[4],
                combinedModuleAccessPointInformation[5]);
    }

    @Override
    public String[] getAccessInformation() {
        return new String[]{String.valueOf(port), username, password, iPAddress};
    }

    @Override
    public int[] getDatabaseIndices() {
        return new int[]{2, 3, 4, 5};
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getiPAddress() {
        return iPAddress;
    }

    public void setiPAddress(String iPAddress) {
        this.iPAddress = iPAddress;
    }

    @Override
    public String toString() {
        return getType() + "#" + getUsername() + "@" + getiPAddress() + ":" + getPort();
    }
}
