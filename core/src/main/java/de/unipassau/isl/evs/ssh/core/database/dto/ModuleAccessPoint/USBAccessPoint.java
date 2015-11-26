package de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint;

/**
 * @author leon
 */
public class USBAccessPoint extends ModuleAccessPoint {
    public static final String TYPE = "USB";
    private int port;

    public USBAccessPoint() {
    }

    public USBAccessPoint(int port) {
        this.port = port;
    }

    @Override
    public String[] getAccessInformation() {
        return new String[] { String.valueOf(port) };
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Create USBAccessPoint from combined access point information.
     * @param combinedModuleAccessPointInformation The combined access point information.
     * @return The constructed USBAccessPoint.
     */
    public static USBAccessPoint fromCombinedModuleAccessPointInformation(
            String[] combinedModuleAccessPointInformation) {
        return new USBAccessPoint(Integer.valueOf(combinedModuleAccessPointInformation[1]));
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public int[] getDatabaseIndices() {

        return new int[] { 1 };
    }
}
