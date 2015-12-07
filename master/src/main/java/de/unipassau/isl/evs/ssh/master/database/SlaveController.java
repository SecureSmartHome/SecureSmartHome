package de.unipassau.isl.evs.ssh.master.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import com.google.common.collect.ObjectArrays;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.ModuleAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * Offers high level methods to interact with the tables associated with slaves and modules in the database.
 * @author leon
 */
public class SlaveController extends AbstractComponent {
    public static final Key<SlaveController> KEY = new Key<>(SlaveController.class);
    private DatabaseConnector databaseConnector;
    private static final String SLAVE_ID_FROM_FINGERPRINT_SQL_QUERY =
            "select " + DatabaseContract.Slave.COLUMN_ID
                    + " from " + DatabaseContract.Slave.TABLE_NAME
                    + " where " + DatabaseContract.Slave.COLUMN_FINGERPRINT
                    + " = ?";

    @Override
    public void init(Container container) {
        super.init(container);
        databaseConnector = requireComponent(DatabaseConnector.KEY);
    }

    @Override
    public void destroy() {
        super.destroy();
        databaseConnector = null;
    }

    /**
     * Create a String array suitable to be inserted into the database. This function will fill in null values for all
     * columns not used by the given ModuleAccessPoint.
     *
     * @param moduleAccessPoint ModulesAccessPoint to create String array from.
     * @return Generated String array.
     */
    private String[] createCombinedModulesAccessInformationFromSingle(
            ModuleAccessPoint moduleAccessPoint) {
        String[] allModuleAccessPoints =
                new String[ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION];
        for (int i = 0; i < allModuleAccessPoints.length; i++) {
            allModuleAccessPoints[i] = "NULL";
        }

        for (int i = 0; i < moduleAccessPoint.getDatabaseIndices().length; i++) {
            allModuleAccessPoints[moduleAccessPoint.getDatabaseIndices()[i]] =
                    moduleAccessPoint.getAccessInformation()[i];
        }
        for (String allModuleAccessPoint : allModuleAccessPoints) {
            System.out.println(allModuleAccessPoint);
        }
        return allModuleAccessPoints;
    }

    /**
     * Add a new Module.
     *
     * @param module Module to add.
     */
    public void addModule(Module module) throws DatabaseControllerException {
        try {
            //Notice: Changed order of values to avoid having to concat twice!
            databaseConnector.executeSql("insert into "
                            + DatabaseContract.ElectronicModule.TABLE_NAME + " ("
                            + DatabaseContract.ElectronicModule.COLUMN_GPIO_PIN + ", "
                            + DatabaseContract.ElectronicModule.COLUMN_USB_PORT + ", "
                            + DatabaseContract.ElectronicModule.COLUMN_WLAN_PORT + ", "
                            + DatabaseContract.ElectronicModule.COLUMN_WLAN_USERNAME + ", "
                            + DatabaseContract.ElectronicModule.COLUMN_WLAN_PASSWORD + ", "
                            + DatabaseContract.ElectronicModule.COLUMN_WLAN_IP + ", "
                            + DatabaseContract.ElectronicModule.COLUMN_MODULE_TYPE + ", "
                            + DatabaseContract.ElectronicModule.COLUMN_CONNECTOR_TYPE + ", "
                            + DatabaseContract.ElectronicModule.COLUMN_SLAVE_ID + ", "
                            + DatabaseContract.ElectronicModule.COLUMN_NAME + ") values "
                            + "(?, ?, ?, ?, ?, ?, ?, ?, (" + SLAVE_ID_FROM_FINGERPRINT_SQL_QUERY + "), ?)",
                    ObjectArrays.concat(
                            createCombinedModulesAccessInformationFromSingle(module.getModuleAccessPoint()),
                            new String[] { module.getModuleType(), module.getModuleAccessPoint().getType(),
                                    module.getAtSlave().getId(), module.getName() }, String.class));
        } catch (SQLiteConstraintException sqlce) {
            throw new DatabaseControllerException("The given Slave does not exist in the database"
                    + " or the name is already used by another Module", sqlce);
        }
    }

    /**
     * Remove a Module.
     *
     * @param moduleName Name of the Module to remove.
     */
    public void removeModule(String moduleName) {
        databaseConnector.executeSql("delete from "
                        + DatabaseContract.ElectronicModule.TABLE_NAME
                        + " where " + DatabaseContract.ElectronicModule.COLUMN_NAME + " = ?",
                            new String[] { moduleName });
    }

    /**
     * Gets a list of all Modules.
     */
    public List<Module> getModules() {
        //Notice: again changed order for convenience reasons when creating the ModuleAccessPoint.
        Cursor modulesCursor = databaseConnector.executeSql("select" +
                " m." + DatabaseContract.ElectronicModule.COLUMN_GPIO_PIN
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_USB_PORT
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_WLAN_PORT
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_WLAN_USERNAME
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_WLAN_PASSWORD
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_WLAN_IP
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_MODULE_TYPE
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_CONNECTOR_TYPE
                + ", s." + DatabaseContract.Slave.COLUMN_FINGERPRINT
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_NAME
                + " from " + DatabaseContract.ElectronicModule.TABLE_NAME + " m"
                + " join " + DatabaseContract.Slave.TABLE_NAME + " s"
                + " on m." + DatabaseContract.ElectronicModule.COLUMN_SLAVE_ID
                + " = s." + DatabaseContract.Slave.COLUMN_ID, new String[] {});
        List<Module> modules = new LinkedList<>();
        while (modulesCursor.moveToNext()) {
            String[] combinedModuleAccessPointInformation =
                    new String[ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION];
            for (int i = 0; i < ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION; i++) {
                combinedModuleAccessPointInformation[i] = modulesCursor.getString(i);
            }
            ModuleAccessPoint moduleAccessPoint = ModuleAccessPoint
                    .fromCombinedModuleAccessPointInformation(combinedModuleAccessPointInformation,
                            modulesCursor.getString(ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION + 1));
            modules.add(new Module(modulesCursor.getString(
                    ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION + 3), new DeviceID(
                    modulesCursor.getString(
                    ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION + 2)),
                    modulesCursor.getString(ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION),
                    moduleAccessPoint));
        }
        return modules;
    }

    /**
     * Get all information of a single Module by name.
     * @param moduleName The name of the Module.
     * @return The requested Module.
     */
    public Module getModule(String moduleName) {
        //Notice: again changed order for convenience reasons when creating the ModuleAccessPoint.
        Cursor moduleCursor = databaseConnector.executeSql("select" +
                " m." + DatabaseContract.ElectronicModule.COLUMN_GPIO_PIN
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_USB_PORT
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_WLAN_PORT
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_WLAN_USERNAME
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_WLAN_PASSWORD
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_WLAN_IP
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_MODULE_TYPE
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_CONNECTOR_TYPE
                + ", s." + DatabaseContract.Slave.COLUMN_FINGERPRINT
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_NAME
                + " from " + DatabaseContract.ElectronicModule.TABLE_NAME + " m"
                + " join " + DatabaseContract.Slave.TABLE_NAME + " s"
                + " on m." + DatabaseContract.ElectronicModule.COLUMN_SLAVE_ID
                + " = s." + DatabaseContract.Slave.COLUMN_ID
                + " where m." + DatabaseContract.ElectronicModule.COLUMN_NAME + " = ?",
                    new String[] { moduleName });
        if (moduleCursor.moveToNext()) {
            String[] combinedModuleAccessPointInformation =
                    new String[ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION];
            for (int i = 0; i < ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION; i++) {
                combinedModuleAccessPointInformation[i] = moduleCursor.getString(i);
            }
            ModuleAccessPoint moduleAccessPoint = ModuleAccessPoint
                    .fromCombinedModuleAccessPointInformation(combinedModuleAccessPointInformation,
                            moduleCursor.getString(ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION + 1));
            return new Module(moduleCursor.getString(
                    ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION + 3), new DeviceID(
                    moduleCursor.getString(ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION + 2)),
                    moduleCursor.getString(ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION),
                    moduleAccessPoint);
        }
        return null;
    }

    /**
     * Get all Modules of a given Slave.
     * @param slaveDeviceID DeviceID of the Slave.
     * @return All Modules of the Slave as a list.
     */
    public List<Module> getModulesOfSlave(DeviceID slaveDeviceID) {
        //Notice: again changed order for convenience reasons when creating the ModuleAccessPoint.
        Cursor modulesCursor = databaseConnector.executeSql("select" +
                " m." + DatabaseContract.ElectronicModule.COLUMN_GPIO_PIN
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_USB_PORT
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_WLAN_PORT
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_WLAN_USERNAME
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_WLAN_PASSWORD
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_WLAN_IP
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_MODULE_TYPE
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_CONNECTOR_TYPE
                + ", s." + DatabaseContract.Slave.COLUMN_FINGERPRINT
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_NAME
                + " from " + DatabaseContract.ElectronicModule.TABLE_NAME + " m"
                + " join " + DatabaseContract.Slave.TABLE_NAME + " s"
                + " on m." + DatabaseContract.ElectronicModule.COLUMN_SLAVE_ID
                + " = s." + DatabaseContract.Slave.COLUMN_ID
                + " where s." + DatabaseContract.Slave.COLUMN_FINGERPRINT + " = ?",
                    new String[] { slaveDeviceID.getId() });
        List<Module> modules = new LinkedList<>();
        while (modulesCursor.moveToNext()) {
            String[] combinedModuleAccessPointInformation =
                    new String[ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION];
            for (int i = 0; i < ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION; i++) {
                combinedModuleAccessPointInformation[i] = modulesCursor.getString(i);
            }
            ModuleAccessPoint moduleAccessPoint = ModuleAccessPoint
                    .fromCombinedModuleAccessPointInformation(combinedModuleAccessPointInformation,
                            modulesCursor.getString(ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION + 1));
            modules.add(new Module(modulesCursor.getString(
                    ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION + 3), new DeviceID(
                    modulesCursor.getString(ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION + 2)),
                    modulesCursor.getString(ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION),
                    moduleAccessPoint));
        }
        return modules;
    }

    /**
     * Change the name of a Module.
     *
     * @param oldName Old Module name.
     * @param newName New Module name.
     */
    public void changeModuleName(String oldName, String newName) throws AlreadyInUseException {
        try {
            databaseConnector.executeSql("update " + DatabaseContract.ElectronicModule.TABLE_NAME
                            + " set " + DatabaseContract.ElectronicModule.COLUMN_NAME
                            + " = ? where " + DatabaseContract.ElectronicModule.COLUMN_NAME + " = ?",
                    new String[] { newName, oldName });
        } catch (SQLiteConstraintException sqlce) {
            throw new AlreadyInUseException("The given name is already used by another Module.", sqlce);
        }
    }

    /**
     * Get a list of all Slaves.
     */
    public List<Slave> getSlaves() {
        Cursor slavesCursor = databaseConnector.executeSql("select "
                        + DatabaseContract.Slave.COLUMN_NAME
                        + ", " + DatabaseContract.Slave.COLUMN_FINGERPRINT
                        + " from " + DatabaseContract.Slave.TABLE_NAME, new String[] {});
        List<Slave> slaves = new LinkedList<>();
        while (slavesCursor.moveToNext()) {
            slaves.add(new Slave(slavesCursor.getString(0),
                    new DeviceID(slavesCursor.getString(1))));
        }
        return slaves;
    }

    /**
     * Change the name of a Slave.
     *
     * @param oldName Old Slave name.
     * @param newName New Slave name.
     */
    public void changeSlaveName(String oldName, String newName) throws AlreadyInUseException {
        try {
            databaseConnector.executeSql("update " + DatabaseContract.Slave.TABLE_NAME
                            + " set " + DatabaseContract.Slave.COLUMN_NAME
                            + " = ? where " + DatabaseContract.Slave.COLUMN_NAME + " = ?",
                    new String[] { newName, oldName });
        } catch (SQLiteConstraintException sqlce) {
            throw new AlreadyInUseException("The given name is already used by another Slave.", sqlce);
        }
    }

    /**
     * Add a new Slave.
     *
     * @param slave Slave to add.
     */
    public void addSlave(Slave slave) throws AlreadyInUseException {
        try {
            databaseConnector.executeSql("insert into "
                            + DatabaseContract.Slave.TABLE_NAME
                            + " (" + DatabaseContract.Slave.COLUMN_NAME
                            + ", " + DatabaseContract.Slave.COLUMN_FINGERPRINT + ") values (?, ?)",
                    new String[]{slave.getName(), slave.getSlaveID().getId()});
        } catch (SQLiteConstraintException sqlce) {
            throw new AlreadyInUseException("The given name or fingerprint is already used by another Slave.", sqlce);
        }
    }

    /**
     * Remove a Slave.
     *
     * @param slaveID DeviceID of the Slave.
     */
    public void removeSlave(DeviceID slaveID) throws IsReferencedException {
        try {
            databaseConnector.executeSql("delete from "
                            + DatabaseContract.Slave.TABLE_NAME
                            + " where " + DatabaseContract.Slave.COLUMN_FINGERPRINT + " = ?",
                    new String[] { slaveID.getId() });
        } catch (SQLiteConstraintException sqlce) {
            throw new IsReferencedException("This slave is used by at least one Module", sqlce);
        }
    }

    public Integer getModuleID(String moduleName) {
        Cursor moduleCursor = databaseConnector.executeSql("select "
                        + DatabaseContract.ElectronicModule.COLUMN_ID
                        + " from " + DatabaseContract.ElectronicModule.TABLE_NAME
                        + " where " + DatabaseContract.ElectronicModule.COLUMN_NAME
                        + " = ?", new String[] { moduleName });
        if (moduleCursor.moveToNext()) {
            return moduleCursor.getInt(0);
        }
        return null;
    }

    public Slave getSlave(DeviceID slaveID) {
        Cursor slavesCursor = databaseConnector.executeSql("select "
                + DatabaseContract.Slave.COLUMN_NAME
                + ", " + DatabaseContract.Slave.COLUMN_FINGERPRINT
                + " from " + DatabaseContract.Slave.TABLE_NAME
                + " where " + DatabaseContract.Slave.COLUMN_FINGERPRINT
                + " = ?", new String[] {slaveID.getIDString()});
        if (slavesCursor.moveToNext()) {
            return new Slave(slavesCursor.getString(0), new DeviceID(slavesCursor.getString(1)));
        }
        return null;
    }

    public List<Module> getModulesByType(String type) {
        //Notice: again changed order for convenience reasons when creating the ModuleAccessPoint.
        Cursor modulesCursor = databaseConnector.executeSql("select" +
                        " m." + DatabaseContract.ElectronicModule.COLUMN_GPIO_PIN
                        + ", m." + DatabaseContract.ElectronicModule.COLUMN_USB_PORT
                        + ", m." + DatabaseContract.ElectronicModule.COLUMN_WLAN_PORT
                        + ", m." + DatabaseContract.ElectronicModule.COLUMN_WLAN_USERNAME
                        + ", m." + DatabaseContract.ElectronicModule.COLUMN_WLAN_PASSWORD
                        + ", m." + DatabaseContract.ElectronicModule.COLUMN_WLAN_IP
                        + ", m." + DatabaseContract.ElectronicModule.COLUMN_MODULE_TYPE
                        + ", m." + DatabaseContract.ElectronicModule.COLUMN_CONNECTOR_TYPE
                        + ", s." + DatabaseContract.Slave.COLUMN_FINGERPRINT
                        + ", m." + DatabaseContract.ElectronicModule.COLUMN_NAME
                        + " from " + DatabaseContract.ElectronicModule.TABLE_NAME + " m"
                        + " join " + DatabaseContract.Slave.TABLE_NAME + " s"
                        + " on m." + DatabaseContract.ElectronicModule.COLUMN_SLAVE_ID
                        + " = s." + DatabaseContract.Slave.COLUMN_ID
                        + " where s." + DatabaseContract.ElectronicModule.COLUMN_MODULE_TYPE + " = ?",
                new String[] { type });
        List<Module> modules = new LinkedList<>();
        while (modulesCursor.moveToNext()) {
            String[] combinedModuleAccessPointInformation =
                    new String[ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION];
            for (int i = 0; i < ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION; i++) {
                combinedModuleAccessPointInformation[i] = modulesCursor.getString(i);
            }
            ModuleAccessPoint moduleAccessPoint = ModuleAccessPoint
                    .fromCombinedModuleAccessPointInformation(combinedModuleAccessPointInformation,
                            modulesCursor.getString(ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION + 1));
            modules.add(new Module(modulesCursor.getString(
                    ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION + 3), new DeviceID(
                    modulesCursor.getString(ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION + 2)),
                    modulesCursor.getString(ModuleAccessPoint.COMBINED_AMOUNT_OF_ACCESS_INFORMATION),
                    moduleAccessPoint));
        }
        return modules;
    }
}