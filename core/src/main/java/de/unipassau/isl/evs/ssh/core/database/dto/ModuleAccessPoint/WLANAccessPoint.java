package de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint;

/**
 * ModuleAccessPoint class for a WLAN access point.
 * @author leon
 */
public class WLANAccessPoint extends ModuleAccessPoint {
    public static final String TYPE = "WLAN";
    private int port;
    private String username;
    private String password;
    private String iPAddress;

    public WLANAccessPoint() {
    }

    public WLANAccessPoint(int port, String username, String password, String iPAddress) {
        this.port = port;
        this.username = username;
        this.password = password;
        this.iPAddress = iPAddress;
    }

    @Override
    public String[] getAccessInformation() {
        return new String[] { String.valueOf(port), username, password, iPAddress };
    }

    @Override
    public int[] getDatabaseIndices() {
        return new int[] { 2, 3, 4, 5 };
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Create WLANAccessPoint from combined access point information.
     * @param combinedModuleAccessPointInformation The combined access point information.
     * @return The constructed WLANAccessPoint.
     */
    public static WLANAccessPoint fromCombinedModuleAccessPointInformation(
            String[] combinedModuleAccessPointInformation) {
        return new WLANAccessPoint(Integer.valueOf(combinedModuleAccessPointInformation[2]),
                combinedModuleAccessPointInformation[3], combinedModuleAccessPointInformation[4],
                combinedModuleAccessPointInformation[5]);
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
}
