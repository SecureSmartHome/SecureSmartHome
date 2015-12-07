package de.unipassau.isl.evs.ssh.core.messaging.payload;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;

/**
 * @author bucher
 */
public class ModulesPayload implements MessagePayload {
    //TODO Core kennt keine Modules, wie also Payload gestalten?
    private List<Module> modules;
    private List<Slave> slaves;

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
}
