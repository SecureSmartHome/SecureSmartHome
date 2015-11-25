package de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint;

/**
 * ModuleAccessPoint class for a GPIO access point.
 * @author leon
 */
public class GPIOAccessPoint extends ModuleAccessPoint {
    public static final String TYPE = "GPIO";
    private int port;

    public GPIOAccessPoint() {
    }

    public GPIOAccessPoint(int port) {
        this.port = port;
    }

    @Override
    public String[] getAccessInformation() {
        return new String[] { String.valueOf(port) };
    }

    @Override
    public int[] getDatabaseIndices() {
        return new int[] { 0 };
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Create GPIOAccessPoint from combined access point information.
     * @param combinedModuleAccessPointInformation The combined access point information.
     * @return The constructed GPIOAccessPoint.
     */
    public static GPIOAccessPoint fromCombinedModuleAccessPointInformation(
            String[] combinedModuleAccessPointInformation) {
        return new GPIOAccessPoint(Integer.valueOf(combinedModuleAccessPointInformation[0]));
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
