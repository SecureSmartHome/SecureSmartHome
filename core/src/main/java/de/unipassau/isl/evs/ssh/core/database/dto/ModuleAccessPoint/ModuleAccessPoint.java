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
    public static int COMBINED_AMOUNT_OF_ACCESS_INFORMATION = 6;

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
