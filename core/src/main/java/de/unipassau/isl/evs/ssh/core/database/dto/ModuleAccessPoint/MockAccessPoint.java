package de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint;

/**
 * Mock ModuleAccessPoint that indicates a Mock driver should be used.
 *
 * @author Niko Fink
 */
public class MockAccessPoint extends ModuleAccessPoint {
    public static final String TYPE = "MOCK";

    @Override
    public String[] getAccessInformation() {
        return new String[0];
    }

    @Override
    public int[] getDatabaseIndices() {
        return new int[0];
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return getType();
    }
}
