package de.unipassau.isl.evs.ssh.master.database;

import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.dto.Module;
import de.unipassau.isl.evs.ssh.master.database.dto.Slave;

/**
 * Offers high level methods to interact with the tables associated with slaves in the database.
 */
public class SlaveController extends AbstractComponent {
    public static final Key<SlaveController> KEY = new Key<>(SlaveController.class);
    private DatabaseConnector databaseConnector;

    @Override
    public void init(Container container) {
        super.init(container);
        databaseConnector = requireComponent(DatabaseConnector.KEY);
    }

    /**
     * Add a new Module.
     *
     * @param module Module to add.
     */
    public void addModule(Module module) {
        // TODO - implement SlaveController.addModule
        throw new UnsupportedOperationException();
    }

    /**
     * Remove a Module.
     *
     * @param moduleName Name of the Module to remove.
     */
    public void removeModule(String moduleName) {
        // TODO - implement SlaveController.removeModule
        throw new UnsupportedOperationException();
    }

    /**
     * Gets a list of all Modules.
     */
    public List<Module> getModules() {
        // TODO - implement SlaveController.getModules
        throw new UnsupportedOperationException();
    }

    /**
     * Change the name of a Module.
     *
     * @param oldName Old Module name.
     * @param newName New Module name.
     */
    public void changeModuleName(String oldName, String newName) {
        // TODO - implement SlaveController.changeModuleName
        throw new UnsupportedOperationException();
    }

    /**
     * Get a list of all Slaves.
     */
    public List<Slave> getSlaves() {
        // TODO - implement SlaveController.getSlaves
        throw new UnsupportedOperationException();
    }

    /**
     * Change the name of a Slave.
     *
     * @param oldName Old Slave name.
     * @param newName New Slave name.
     */
    public void changeSlaveName(String oldName, String newName) {
        // TODO - implement SlaveController.changeSlaveName
        throw new UnsupportedOperationException();
    }

    /**
     * Add a new Slave.
     *
     * @param slave Slave to add.
     */
    public void addSlave(Slave slave) {

    }

    /**
     * Remove a Slave.
     *
     * @param slaveID DeviceID of the Slave.
     */
    public void removeSlave(DeviceID slaveID) {
        // TODO - implement SlaveController.removeSlave
        throw new UnsupportedOperationException();
    }

}