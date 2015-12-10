package de.unipassau.isl.evs.ssh.core.database.dto;

/**
 * TODO comment
 *
 * @author Christoph Fraedrich
 */
public class HolidayAction {

    private String moduleName;
    private long timeStamp;
    private String actionName;

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
}
