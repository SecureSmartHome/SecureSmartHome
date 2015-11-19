package de.unipassau.isl.evs.ssh.master.database.dto.ModuleAccessPoint;

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
        return new String[0];
    }

    @Override
    public int[] getDatabaseIndices() {
        return new int[] { 0 };
    }

    @Override
    public String getType() {
        return TYPE;
    }

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
