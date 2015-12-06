package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;

/**
 * Payload class for SystemHealthChecker
 *
 * @author bucher
 */
public class SystemHealthPayload {

    private boolean hasError;
    private Module module;

    public SystemHealthPayload (boolean hasError, Module defectModule) {
        this.hasError = hasError;
        this.module = defectModule;
    }

    /**
     * Returns true if a Module is not working properly.
     *
     * @return boolean defect
     */
    public boolean getHasError() {
        return hasError;
    }

    /**
     * Returns the Module that is not working properly to notify the user about ist.
     *
     * @return defect Module
     */
    public Module getModule() {
        return module;
    }
}
