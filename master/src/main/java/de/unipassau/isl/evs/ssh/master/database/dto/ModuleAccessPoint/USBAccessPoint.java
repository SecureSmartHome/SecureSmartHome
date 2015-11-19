package de.unipassau.isl.evs.ssh.master.database.dto.ModuleAccessPoint;

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
        return new String[0];
    }

    @Override
    public String getType() {
        return TYPE;
    }

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
