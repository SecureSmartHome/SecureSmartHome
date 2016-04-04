/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unipassau.isl.evs.ssh.core.database.dto;

/**
 * A HolidayAction describes an action that happened in the past and shall be re-executed
 * when the HolidaySimulation is active. It describes which module {@link #moduleName} executed which
 * action {@link #actionName} at what time {@link #timeStamp}.
 *
 * @author Christoph Fraedrich
 */
public class HolidayAction {
    private final String moduleName;
    private final long timeStamp;
    private final String actionName;

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

    /**
     * Creates a new HolidayAction Object using the current time as timestamp
     *
     * @param moduleName of the module which is to be triggered
     * @param actionName action which is to be executed
     */
    public HolidayAction(String moduleName, String actionName) {
        this.moduleName = moduleName;
        this.actionName = actionName;
        timeStamp = System.currentTimeMillis();
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
