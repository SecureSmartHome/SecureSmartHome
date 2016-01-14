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

    private final List<Slave> slaves;
    private final ListMultimap<Slave, Module> modulesAtSlave;

    /**
     * Constructs a new ModulesPayload.
     *
     * @param modulesAtSlave a map describing which modules are connected to which slaves
     * @param slaves         a list of all slaves
     */
    public ModulesPayload(ListMultimap<Slave, Module> modulesAtSlave, List<Slave> slaves) {
        this.modulesAtSlave = modulesAtSlave;
        this.slaves = slaves;
    }

    /**
     * Gets all modules that are available in the system.
     *
     * @return a set of all modules
     */
    public Set<Module> getModules() {
        return Sets.newHashSet(modulesAtSlave.values());
    }

    /**
     * Gets all slaves that are registered in the system.
     *
     * @return a list of all slaves
     */
    public List<Slave> getSlaves() {
        return slaves;
    }

    /**
     * Gets the modules that are connected to the given slave.
     *
     * @param slave the slave whose modules are queried
     * @return a list of all modules connected to the given slave
     */
    public List<Module> getModulesAtSlave(Slave slave) {
        return modulesAtSlave.get(slave);
    }

     /**
     * Gets the modules that are connected to the given slave.
     *
     * @param slaveID the slave id whose modules are queried
     * @return a list of all modules connected to the given slave
     */
    public List<Module> getModulesAtSlave(DeviceID slaveID) {
        if (getSlave(slaveID) == null) {
            return null;
        }
        return modulesAtSlave.get(getSlave(slaveID));
    }

    /**
     * Gets the slave DTO for the given slave id.
     *
     * @param slaveID the slave id
     * @return the slave DTO of the given id
     */
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

    /**
     * Gets the which modules are connected to which slaves.
     *
     * @return a map describing which modules are connected to which slaves
     */
    public ListMultimap<Slave, Module> getModulesAtSlaves() {
        return modulesAtSlave;
    }
}
