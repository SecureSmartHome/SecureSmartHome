package de.unipassau.isl.evs.ssh.core.messaging.payload;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;

/**
 * @author bucher
 */
public class ModulesPayload implements MessagePayload {
    //TODO Core kennt keine Modules, wie also Payload gestalten?
    private List<Module> modules;

    public ModulesPayload(List<Module> modules) {
        this.modules = modules;
    }
}
