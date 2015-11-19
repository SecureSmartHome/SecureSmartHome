package de.unipassau.isl.evs.ssh.master.database.dto.ModuleAccessPoint;

public abstract class ModuleAccessPoint {
    public static int COMBINED_AMOUNT_OF_ACCESS_INFORMATION = 6;

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
            default:
                throw new IllegalArgumentException("Type " + type + "is not supported.");
        }
    }

    public abstract String[] getAccessInformation();
    public abstract int[] getDatabaseIndices();
    public abstract String getType();
}
