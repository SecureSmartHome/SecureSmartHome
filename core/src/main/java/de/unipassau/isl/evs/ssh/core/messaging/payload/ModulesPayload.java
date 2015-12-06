package de.unipassau.isl.evs.ssh.core.messaging.payload;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;

/**
 * The ModulesPayload is a payload used for exchanging information on modules and slaves.
 *
 * @author bucher
 * @author Wolfgang Popp
 */
public class ModulesPayload implements MessagePayload {

    private List<Module> modules;
    private List<Slave> slaves;

    /**
     * Constructs a new and empty Modules Payload.
     */
    public ModulesPayload(){
        this.modules = null;
        this.slaves = null;
    }

    public ModulesPayload(List<Module> modules, List<Slave> slaves) {
        this.modules = modules;
        this.slaves = slaves;
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Slave> getSlaves() {
        return slaves;
    }

    public void setModules(List<Module> modules) {
        this.modules = modules;
    }

    public void setSlaves(List<Slave> slaves) {
        this.slaves = slaves;
    }
}
