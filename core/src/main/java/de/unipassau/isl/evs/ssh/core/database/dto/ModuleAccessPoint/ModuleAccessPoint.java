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

import java.io.Serializable;
import java.util.Arrays;

/**
 * Defines how to access how Module at a given Slave.
 *
 * @author Leon Sell
 */
public abstract class ModuleAccessPoint implements Serializable {
    //Amount all different possible entries for access information in the database.
    public static final int COMBINED_AMOUNT_OF_ACCESS_INFORMATION = 6;

    /**
     * Builds a ModuleAccessPoint object from the given information and type.
     *
     * @param combinedModuleAccessPointInformation Combined access information.
     * @param type                                 Type of access.
     * @return The constructed ModuleAccessPoint.
     */
    public static ModuleAccessPoint fromCombinedModuleAccessPointInformation(
            String[] combinedModuleAccessPointInformation, String type) {
        switch (type) {
            case GPIOAccessPoint.TYPE:
                return GPIOAccessPoint.fromCombinedModuleAccessPointInformation(
                        combinedModuleAccessPointInformation);
            case USBAccessPoint.TYPE:
                return USBAccessPoint.fromCombinedModuleAccessPointInformation(
                        combinedModuleAccessPointInformation);
            case WLANAccessPoint.TYPE:
                return WLANAccessPoint.fromCombinedModuleAccessPointInformation(
                        combinedModuleAccessPointInformation);
            case MockAccessPoint.TYPE:
                return new MockAccessPoint();
            default:
                throw new IllegalArgumentException("Type " + type + "is not supported.");
        }
    }

    /**
     * Get all the access information of the specific ModuleAccessPoint as a String array.
     *
     * @return Access information as String array.
     */
    public abstract String[] getAccessInformation();

    /**
     * Get all indices of the points to write the information to (on the database).
     *
     * @return The indices.
     */
    public abstract int[] getDatabaseIndices();

    /**
     * Get the type of the ModuleAccessPoint object.
     *
     * @return Type of ModuleAccessPoint object.
     */
    public abstract String getType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModuleAccessPoint)) return false;

        ModuleAccessPoint that = (ModuleAccessPoint) o;

        if (!Arrays.equals(getAccessInformation(), that.getAccessInformation())) return false;
        if (!Arrays.equals(getDatabaseIndices(), that.getDatabaseIndices())) return false;
        return !(getType() != null ? !getType().equals(that.getType()) : that.getType() != null);

    }

    @Override
    public int hashCode() {
        int result = getAccessInformation() != null ? Arrays.hashCode(getAccessInformation()) : 0;
        result = 31 * result + (getDatabaseIndices() != null ? Arrays.hashCode(getDatabaseIndices()) : 0);
        result = 31 * result + (getType() != null ? getType().hashCode() : 0);
        return result;
    }
}
