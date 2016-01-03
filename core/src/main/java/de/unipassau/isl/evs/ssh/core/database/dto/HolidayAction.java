package de.unipassau.isl.evs.ssh.core.database.dto;

/**
 * A HolidayAction describes an action that happened in the past and shall be re-executed
 * when the HolidaySimulation is active. It describes which module {@link #moduleName} executed which
 * action {@link #actionName} at what time {@link #timeStamp}.
 *
 * @author Christoph Fraedrich
 */
public class HolidayAction {
    private String moduleName;
    private long timeStamp;
    private String actionName;

    /**
     * Creates a new HolidayAction Object
     *
     * @param moduleName of the module which is to be triggered
     * @param timeStamp at which the action originally happened
     * @param actionName action which is to be executed
     */
    public HolidayAction(String moduleName, long timeStamp, String actionName) {
        this.moduleName = moduleName;
        this.timeStamp = timeStamp;
        this.actionName = actionName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getActionName() {
        return actionName;
    }

    @Override
    public String toString() {
        return "HolidayAction{module='" + moduleName + "', action='" + actionName + "', timeStamp=" + timeStamp + "}";
    }
}
