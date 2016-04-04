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

import java.io.Serializable;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.ModuleAccessPoint;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * A DTO representing modules which are connected to slave devices, e..g. a light.
 *
 * @author Leon Sell
 */
public class Module implements Serializable, NamedDTO {
    private String name;
    private DeviceID atSlave;
    private CoreConstants.ModuleType moduleType;
    private ModuleAccessPoint moduleAccessPoint;

    public Module() {
    }

    public Module(String name, DeviceID atSlave, CoreConstants.ModuleType moduleType, ModuleAccessPoint moduleAccessPoint) {
        this.name = name;
        this.atSlave = atSlave;
        this.moduleType = moduleType;
        this.moduleAccessPoint = moduleAccessPoint;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DeviceID getAtSlave() {
        return atSlave;
    }

    public void setAtSlave(DeviceID atSlave) {
        this.atSlave = atSlave;
    }

    public CoreConstants.ModuleType getModuleType() {
        return moduleType;
    }

    public void setModuleType(CoreConstants.ModuleType moduleType) {
        this.moduleType = moduleType;
    }

    public ModuleAccessPoint getModuleAccessPoint() {
        return moduleAccessPoint;
    }

    public void setModuleAccessPoint(ModuleAccessPoint moduleAccessPoint) {
        this.moduleAccessPoint = moduleAccessPoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Module)) return false;

        Module module = (Module) o;

        if (name != null ? !name.equals(module.name) : module.name != null) return false;
        if (atSlave != null ? !atSlave.equals(module.atSlave) : module.atSlave != null) return false;
        if (moduleType != null ? !moduleType.equals(module.moduleType) : module.moduleType != null) return false;
        return !(moduleAccessPoint != null ? !moduleAccessPoint.equals(module.moduleAccessPoint) : module.moduleAccessPoint != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (atSlave != null ? atSlave.hashCode() : 0);
        result = 31 * result + (moduleType != null ? moduleType.hashCode() : 0);
        result = 31 * result + (moduleAccessPoint != null ? moduleAccessPoint.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Module " + name + "{type " + moduleType + " at " + atSlave.toShortString() + " via " + moduleAccessPoint + '}';
    }
}