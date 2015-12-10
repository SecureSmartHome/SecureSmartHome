package de.unipassau.isl.evs.ssh.core.messaging.payload;

import android.support.annotation.Nullable;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * The ModulesPayload is a payload used for exchanging information on modules and slaves.
 *
 * @author Andreas Bucher
 * @author Wolfgang Popp
 */
public class ModulesPayload implements MessagePayload {

    private List<Slave> slaves;
    private ListMultimap<Slave, Module> modulesAtSlave;

    /**
     * Constructs a new and empty Modules Payload.
     */
    public ModulesPayload() {
        this.modulesAtSlave = null;
    }

    public ModulesPayload(ListMultimap<Slave, Module> modulesAtSlave, List<Slave> slaves) {
        this.modulesAtSlave = modulesAtSlave;
        this.slaves = slaves;
    }

    public Set<Module> getModules() {
        return Sets.newHashSet(modulesAtSlave.values());
    }

    public List<Slave> getSlaves() {
        return slaves;
    }

    public List<Module> getModulesAtSlave(Slave slave) {
        return modulesAtSlave.get(slave);
    }

    public List<Module> getModulesAtSlave(DeviceID slaveID) {
        if (getSlave(slaveID) == null) {
            return null;
        }
        return modulesAtSlave.get(getSlave(slaveID));
    }

    @Nullable
    public Slave getSlave(final DeviceID slaveID) {
        final List<Slave> slaves = getSlaves();
        for (Slave slave : slaves) {
            if (slave.getSlaveID().equals(slaveID)) {
                return slave;
            }
        }
        return null;
    }

    public ListMultimap<Slave, Module> getModulesAtSlaves() {
        return modulesAtSlave;
    }
}
