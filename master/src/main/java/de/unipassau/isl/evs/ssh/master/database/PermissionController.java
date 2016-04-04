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

package de.unipassau.isl.evs.ssh.master.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import com.google.common.base.Strings;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.AccessLogger;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.AlreadyInUseException;
import de.unipassau.isl.evs.ssh.core.database.DatabaseControllerException;
import de.unipassau.isl.evs.ssh.core.database.IsReferencedException;
import de.unipassau.isl.evs.ssh.core.database.UnknownReferenceException;
import de.unipassau.isl.evs.ssh.core.database.dto.PermissionDTO;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * Offers high level methods to interact with the tables associated with permissions in the database.
 * and simply take ({@link de.unipassau.isl.evs.ssh.core.sec.Permission} permission, {@link String} moduleName) instead (Niko, 2015-12-20)
 *
 * @author Leon Sell
 */
public class PermissionController extends AbstractComponent {
    public static final Key<PermissionController> KEY = new Key<>(PermissionController.class);
    private DatabaseConnector databaseConnector;

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
     * Lists all Permissions of a given template.
     *
     * @param templateName Name of the template.
     * @return List of the Permissions in the template.
     */
    public List<PermissionDTO> getPermissionsOfTemplate(String templateName) {
        Cursor permissionsCursor = databaseConnector
                .executeSql("select p." + DatabaseContract.Permission.COLUMN_NAME
                        + ", m." + DatabaseContract.ElectronicModule.COLUMN_NAME
                        + " from " + DatabaseContract.ComposedOfPermission.TABLE_NAME + " comp "
                        + "join " + DatabaseContract.PermissionTemplate.TABLE_NAME + " pt on comp."
                        + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_TEMPLATE_ID
                        + " = pt." + DatabaseContract.PermissionTemplate.COLUMN_ID
                        + " join " + DatabaseContract.Permission.TABLE_NAME
                        + " p on comp." + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_ID
                        + " = p." + DatabaseContract.Permission.COLUMN_ID
                        + " join " + DatabaseContract.ElectronicModule.TABLE_NAME + " m"
                        + " on p." + DatabaseContract.Permission.COLUMN_ELECTRONIC_MODULE_ID
                        + " = m." + DatabaseContract.ElectronicModule.COLUMN_ID
                        + " where pt." + DatabaseContract.PermissionTemplate.COLUMN_NAME
                        + " = ?", new String[]{templateName});
        List<PermissionDTO> permissions = new LinkedList<>();
        while (permissionsCursor.moveToNext()) {
            permissions.add(new PermissionDTO(
                    de.unipassau.isl.evs.ssh.core.sec.Permission.valueOf(permissionsCursor.getString(0)),
                    permissionsCursor.getString(1)
            ));
        }
        permissionsCursor = databaseConnector
                .executeSql("select p." + DatabaseContract.Permission.COLUMN_NAME
                                + " from " + DatabaseContract.ComposedOfPermission.TABLE_NAME + " comp "
                                + "join " + DatabaseContract.PermissionTemplate.TABLE_NAME + " pt on comp."
                                + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_TEMPLATE_ID
                                + " = pt." + DatabaseContract.PermissionTemplate.COLUMN_ID
                                + " join " + DatabaseContract.Permission.TABLE_NAME
                                + " p on comp." + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_ID
                                + " = p." + DatabaseContract.Permission.COLUMN_ID
                                + " where pt." + DatabaseContract.PermissionTemplate.COLUMN_NAME
                                + " = ? and " + DatabaseContract.Permission.COLUMN_ELECTRONIC_MODULE_ID + " is NULL",
                        new String[]{templateName});
        while (permissionsCursor.moveToNext()) {
            permissions.add(new PermissionDTO(
                    de.unipassau.isl.evs.ssh.core.sec.Permission.valueOf(permissionsCursor.getString(0))
            ));
        }
        return permissions;
    }

    /**
     * Returns whether a given user has a given Permission.
     *
     * @param userDeviceID DeviceID associated with the user.
     * @param permission Permission to check.
     * @param moduleName Module the permission applies for.
     * @return true if has permissions otherwise false.
     */
    public boolean hasPermission(DeviceID userDeviceID, de.unipassau.isl.evs.ssh.core.sec.Permission permission, String moduleName) {
        final AccessLogger logger = getComponent(AccessLogger.KEY);
        if (logger != null) {
            logger.logAccess(permission);
        }

        Cursor permissionCursor;
        if (Strings.isNullOrEmpty(moduleName)) {
            permissionCursor = databaseConnector
                    .executeSql(
                            "select * from " + DatabaseContract.HasPermission.TABLE_NAME
                            + " hp join " + DatabaseContract.Permission.TABLE_NAME + " p on "
                            + "hp." + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID
                            + " = p." + DatabaseContract.Permission.COLUMN_ID
                            + " join " + DatabaseContract.UserDevice.TABLE_NAME
                            + " ud on hp." + DatabaseContract.HasPermission.COLUMN_USER_ID
                            + " = ud." + DatabaseContract.UserDevice.COLUMN_ID
                            + " where p." + DatabaseContract.Permission.COLUMN_NAME
                            + " = ? and p." + DatabaseContract.Permission.COLUMN_ELECTRONIC_MODULE_ID
                            + " is NULL and ud." + DatabaseContract.UserDevice.COLUMN_FINGERPRINT
                            + " = ?", new String[]{ permission.toString(), userDeviceID.getIDString() }
                    );
        } else {
            permissionCursor = databaseConnector
                    .executeSql(
                            "select * from " + DatabaseContract.HasPermission.TABLE_NAME
                            + " hp join " + DatabaseContract.Permission.TABLE_NAME + " p on "
                            + "hp." + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID
                            + " = p." + DatabaseContract.Permission.COLUMN_ID
                            + " join " + DatabaseContract.UserDevice.TABLE_NAME
                            + " ud on hp." + DatabaseContract.HasPermission.COLUMN_USER_ID
                            + " = ud." + DatabaseContract.UserDevice.COLUMN_ID
                            + " join " + DatabaseContract.ElectronicModule.TABLE_NAME + " m"
                            + " on p." + DatabaseContract.Permission.COLUMN_ELECTRONIC_MODULE_ID
                            + " = m." + DatabaseContract.ElectronicModule.COLUMN_ID
                            + " where p." + DatabaseContract.Permission.COLUMN_NAME
                            + " = ? and m." + DatabaseContract.ElectronicModule.COLUMN_NAME
                            + " = ? and ud." + DatabaseContract.UserDevice.COLUMN_FINGERPRINT
                            + " = ?", new String[] { permission.toString(), moduleName, userDeviceID.getIDString() }
                    );
        }
        return permissionCursor.moveToNext();
    }

    /**
     * Removes a template from the database.
     *
     * @param templateName Name of the template.
     */
    public void removeTemplate(String templateName) throws IsReferencedException {
        try {
            databaseConnector.executeSql("delete from "
                    + DatabaseContract.PermissionTemplate.TABLE_NAME
                    + " where " + DatabaseContract.PermissionTemplate.COLUMN_NAME
                    + " = ?", new String[]{templateName});
        } catch (SQLiteConstraintException sqlce) {
            throw new IsReferencedException("This template is used by at least one Group", sqlce);
        }
    }

    /**
     * Add a Permission to a Template.
     *
     * @param templateName Name of the Template.
     * @param permission Permission to add.
     * @param moduleName Module the permission applies for.
     */
    public void addPermissionToTemplate(String templateName, de.unipassau.isl.evs.ssh.core.sec.Permission permission,
                                        String moduleName)
            throws UnknownReferenceException {
        try {
            if (Strings.isNullOrEmpty(moduleName)) {
                databaseConnector.executeSql("insert into "
                                + DatabaseContract.ComposedOfPermission.TABLE_NAME
                                + " (" + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_ID
                                + ", " + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_TEMPLATE_ID
                                + ") values ((" + DatabaseContract.SqlQueries.PERMISSION_ID_FROM_NAME_WITHOUT_MODULE_SQL_QUERY
                                + "), (" + DatabaseContract.SqlQueries.TEMPLATE_ID_FROM_NAME_SQL_QUERY + "))",
                        new String[] { permission.toString(), templateName }
                );
            } else {
                databaseConnector.executeSql("insert into "
                                + DatabaseContract.ComposedOfPermission.TABLE_NAME
                                + " (" + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_ID
                                + ", " + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_TEMPLATE_ID
                                + ") values ((" + DatabaseContract.SqlQueries.PERMISSION_ID_FROM_NAME_AND_MODULE_SQL_QUERY
                                + "), (" + DatabaseContract.SqlQueries.TEMPLATE_ID_FROM_NAME_SQL_QUERY + "))",
                        new String[] { permission.toString(), moduleName, templateName }
                );
            }
        } catch (SQLiteConstraintException sqlce) {
            throw new UnknownReferenceException("The given Template or PermissionDTO does not exist in the database",
                    sqlce);
        }
    }

    /**
     * Remove a Permission from a Template.
     *
     * @param templateName Name of the Template.
     * @param permission Permission to remove.
     * @param moduleName Module the permission applies for.
     */
    public void removePermissionFromTemplate(String templateName, de.unipassau.isl.evs.ssh.core.sec.Permission permission, String moduleName) {
        if (Strings.isNullOrEmpty(moduleName)) {
            databaseConnector.executeSql("delete from "
                            + DatabaseContract.ComposedOfPermission.TABLE_NAME
                            + " where " + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_ID
                            + " = (" + DatabaseContract.SqlQueries.PERMISSION_ID_FROM_NAME_WITHOUT_MODULE_SQL_QUERY + ") and "
                            + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_TEMPLATE_ID
                            + " = (" + DatabaseContract.SqlQueries.TEMPLATE_ID_FROM_NAME_SQL_QUERY + ")",
                    new String[] { permission.toString(), templateName }
            );
        } else {
            databaseConnector.executeSql("delete from "
                            + DatabaseContract.ComposedOfPermission.TABLE_NAME
                            + " where " + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_ID
                            + " = (" + DatabaseContract.SqlQueries.PERMISSION_ID_FROM_NAME_AND_MODULE_SQL_QUERY + ") and "
                            + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_TEMPLATE_ID
                            + " = (" + DatabaseContract.SqlQueries.TEMPLATE_ID_FROM_NAME_SQL_QUERY + ")",
                    new String[] { permission.toString(), moduleName, templateName }
            );
        }
    }

    /**
     * Add a Permission for a UserDevice.
     *
     * @param userDeviceID DeviceID of the UserDevice.
     * @param permission Permission to add.
     * @param moduleName Module the permission applies for.
     */
    public void addUserPermission(DeviceID userDeviceID, de.unipassau.isl.evs.ssh.core.sec.Permission permission,
                                  String moduleName) throws UnknownReferenceException {
        try {
            if (Strings.isNullOrEmpty(moduleName)) {
                databaseConnector.executeSql("insert into "
                        + DatabaseContract.HasPermission.TABLE_NAME
                        + " (" + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID
                        + ", " + DatabaseContract.HasPermission.COLUMN_USER_ID
                        + ") values ((" + DatabaseContract.SqlQueries.PERMISSION_ID_FROM_NAME_WITHOUT_MODULE_SQL_QUERY
                        + "), (" + DatabaseContract.SqlQueries.USER_DEVICE_ID_FROM_FINGERPRINT_SQL_QUERY
                        + "))", new String[] { permission.toString(), userDeviceID.getIDString() }
                );
            } else {
                databaseConnector.executeSql("insert into "
                        + DatabaseContract.HasPermission.TABLE_NAME
                        + " (" + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID
                        + ", " + DatabaseContract.HasPermission.COLUMN_USER_ID
                        + ") values ((" + DatabaseContract.SqlQueries.PERMISSION_ID_FROM_NAME_AND_MODULE_SQL_QUERY
                        + "), (" + DatabaseContract.SqlQueries.USER_DEVICE_ID_FROM_FINGERPRINT_SQL_QUERY
                        + "))", new String[] { permission.toString(), moduleName, userDeviceID.getIDString() }
                );
            }
        } catch (SQLiteConstraintException sqlce) {
            throw new UnknownReferenceException(
                    "The given UserDevice or Permission does not exist in the database", sqlce);
        }
    }

    /**
     * Remove a Permission for a UserDevice.
     *
     * @param userDeviceID DeviceID of the UserDevice.
     * @param permission Permission to remove.
     * @param moduleName Module the permission applies for.
     */
    public void removeUserPermission(DeviceID userDeviceID, de.unipassau.isl.evs.ssh.core.sec.Permission permission,
                                     String moduleName) {
        if (Strings.isNullOrEmpty(moduleName)) {
            databaseConnector.executeSql("delete from "
                    + DatabaseContract.HasPermission.TABLE_NAME
                    + " where " + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID
                    + " = (" + DatabaseContract.SqlQueries.PERMISSION_ID_FROM_NAME_WITHOUT_MODULE_SQL_QUERY
                    + ") and " + DatabaseContract.HasPermission.COLUMN_USER_ID
                    + " = (" + DatabaseContract.SqlQueries.USER_DEVICE_ID_FROM_FINGERPRINT_SQL_QUERY
                    + ")", new String[] { permission.toString(), userDeviceID.getIDString() }
            );
        } else {
            databaseConnector.executeSql("delete from "
                    + DatabaseContract.HasPermission.TABLE_NAME
                    + " where " + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID
                    + " = (" + DatabaseContract.SqlQueries.PERMISSION_ID_FROM_NAME_AND_MODULE_SQL_QUERY
                    + ") and " + DatabaseContract.HasPermission.COLUMN_USER_ID
                    + " = (" + DatabaseContract.SqlQueries.USER_DEVICE_ID_FROM_FINGERPRINT_SQL_QUERY
                    + ")", new String[] { permission.toString(), moduleName, userDeviceID.getIDString() }
            );
        }
    }


    /**
     * Adds a new template to the database.
     *
     * @param templateName Name of the template.
     */
    public void addTemplate(String templateName) throws AlreadyInUseException {
        try {
            databaseConnector.executeSql("insert into "
                    + DatabaseContract.PermissionTemplate.TABLE_NAME
                    + " (" + DatabaseContract.PermissionTemplate.COLUMN_NAME + ")"
                    + "values (?)", new String[]{templateName});
        } catch (SQLiteConstraintException sqlce) {
            throw new AlreadyInUseException("The name is already used by another Template.", sqlce);
        }
    }

    /**
     * Adds a new Permission to the database.
     *
     * @param permission Permission to add.
     * @param moduleName Module the permission applies for.
     */
    public void addPermission(de.unipassau.isl.evs.ssh.core.sec.Permission permission,
                              String moduleName) throws DatabaseControllerException {
        try {
            if (Strings.isNullOrEmpty(moduleName)) {
                databaseConnector.executeSql("insert into "
                        + DatabaseContract.Permission.TABLE_NAME
                        + " (" + DatabaseContract.Permission.COLUMN_NAME + ")"
                        + " values (?)", new String[] { permission.toString() }
                );
            } else {
                databaseConnector.executeSql("insert into "
                                + DatabaseContract.Permission.TABLE_NAME
                                + " (" + DatabaseContract.Permission.COLUMN_NAME
                                + ", " + DatabaseContract.Permission.COLUMN_ELECTRONIC_MODULE_ID + ")"
                                + "values (?, (" + DatabaseContract.SqlQueries.MODULE_ID_FROM_NAME_SQL_QUERY + "))",
                        new String[] { permission.toString(), moduleName }
                );
            }
        } catch (SQLiteConstraintException sqlce) {
            throw new DatabaseControllerException("Either the name-module combination is already exists in the database"
                    + " or the given module doesn't exist.", sqlce);
        }
    }

    /**
     * Removes a Permission from the database.
     *
     * @param permission Permission to remove.
     * @param moduleName Module the permission applies for.
     */
    public void removePermission(de.unipassau.isl.evs.ssh.core.sec.Permission permission, String moduleName) {
        if (Strings.isNullOrEmpty(moduleName)) {
            databaseConnector.executeSql("delete from "
                            + DatabaseContract.Permission.TABLE_NAME
                            + " where " + DatabaseContract.Permission.COLUMN_NAME
                            + " = ? and " + DatabaseContract.Permission.COLUMN_ELECTRONIC_MODULE_ID + " is NULL",
                    new String[] { permission.toString() }
            );
        } else {
            databaseConnector.executeSql("delete from "
                            + DatabaseContract.Permission.TABLE_NAME
                            + " where " + DatabaseContract.Permission.COLUMN_NAME
                            + " = ? and " + DatabaseContract.Permission.COLUMN_ELECTRONIC_MODULE_ID
                            + " = (" + DatabaseContract.SqlQueries.MODULE_ID_FROM_NAME_SQL_QUERY + ")",
                    new String[] { permission.toString(), moduleName }
            );
        }
    }

    /**
     * Get the names of all Permissions.
     *
     * @return All names as a list.
     */
    public List<PermissionDTO> getPermissions() {
        //Permissions with module
        Cursor permissionsCursor = databaseConnector.executeSql("select p."
                + DatabaseContract.Permission.COLUMN_NAME
                + ", m." + DatabaseContract.ElectronicModule.COLUMN_NAME
                + " from " + DatabaseContract.Permission.TABLE_NAME
                + " p join " + DatabaseContract.ElectronicModule.TABLE_NAME
                + " m on p." + DatabaseContract.Permission.COLUMN_ELECTRONIC_MODULE_ID
                + " = m." + DatabaseContract.ElectronicModule.COLUMN_ID, new String[]{});
        List<PermissionDTO> permissions = new LinkedList<>();
        while (permissionsCursor.moveToNext()) {
            permissions.add(new PermissionDTO(
                    de.unipassau.isl.evs.ssh.core.sec.Permission.valueOf(permissionsCursor.getString(0)),
                    permissionsCursor.getString(1)
            ));
        }
        //Permissions without modules
        permissionsCursor = databaseConnector.executeSql("select " + DatabaseContract.Permission.COLUMN_NAME
                + " from " + DatabaseContract.Permission.TABLE_NAME
                + " where " + DatabaseContract.Permission.COLUMN_ELECTRONIC_MODULE_ID + " is NULL", new String[]{});
        while (permissionsCursor.moveToNext()) {
            permissions.add(new PermissionDTO(de.unipassau.isl.evs.ssh.core.sec.Permission.valueOf(
                    permissionsCursor.getString(0)
            )));
        }
        return permissions;
    }

    /**
     * Get the name of all Templates.
     *
     * @return All names as a list.
     */
    public List<String> getTemplates() {
        Cursor templatesCursor = databaseConnector.executeSql("select "
                + DatabaseContract.PermissionTemplate.COLUMN_NAME
                + " from " + DatabaseContract.PermissionTemplate.TABLE_NAME, new String[]{});
        List<String> templates = new LinkedList<>();
        while (templatesCursor.moveToNext()) {
            templates.add(templatesCursor.getString(0));
        }
        return templates;
    }

    /**
     * Change the name of a Template.
     *
     * @param oldName Old name of the Template.
     * @param newName New name of the Template.
     * @throws AlreadyInUseException
     */
    public void changeTemplateName(String oldName, String newName) throws AlreadyInUseException {
        try {
            databaseConnector.executeSql("update " + DatabaseContract.PermissionTemplate.TABLE_NAME
                            + " set " + DatabaseContract.PermissionTemplate.COLUMN_NAME
                            + " = ? where " + DatabaseContract.PermissionTemplate.COLUMN_NAME + " = ?",
                    new String[]{newName, oldName});
        } catch (SQLiteConstraintException sqlce) {
            throw new AlreadyInUseException("The given name is already used by another Template.", sqlce);
        }
    }

    /**
     * Returns all UserDevices that have a given permission.
     *
     * @param permission Permission to check for.
     * @param moduleName Module the permission applies for.
     * @return List of the UserDevices.
     */
    public List<UserDevice> getAllUserDevicesWithPermission(de.unipassau.isl.evs.ssh.core.sec.Permission permission, String moduleName) {
        Cursor userDevicesCursor;
        if (Strings.isNullOrEmpty(moduleName)) {
            userDevicesCursor = databaseConnector.executeSql("select"
                    + " u." + DatabaseContract.UserDevice.COLUMN_NAME
                    + ", u." + DatabaseContract.UserDevice.COLUMN_FINGERPRINT
                    + ", g." + DatabaseContract.Group.COLUMN_NAME
                    + " from " + DatabaseContract.HasPermission.TABLE_NAME + " hp"
                    + " join " + DatabaseContract.Permission.TABLE_NAME + " p"
                    + " on hp." + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID
                    + " = p." + DatabaseContract.Permission.COLUMN_ID
                    + " join " + DatabaseContract.UserDevice.TABLE_NAME + " u"
                    + " on hp." + DatabaseContract.HasPermission.COLUMN_USER_ID
                    + " = u." + DatabaseContract.UserDevice.COLUMN_ID
                    + " join " + DatabaseContract.Group.TABLE_NAME + " g"
                    + " on u." + DatabaseContract.UserDevice.COLUMN_GROUP_ID
                    + " = g." + DatabaseContract.Group.COLUMN_ID
                    + " where p." + DatabaseContract.Permission.COLUMN_NAME
                    + " = ? and p." + DatabaseContract.Permission.COLUMN_ELECTRONIC_MODULE_ID
                    + " is NULL", new String[] { permission.toString() }
            );
        } else {
            userDevicesCursor = databaseConnector.executeSql("select"
                    + " u." + DatabaseContract.UserDevice.COLUMN_NAME
                    + ", u." + DatabaseContract.UserDevice.COLUMN_FINGERPRINT
                    + ", g." + DatabaseContract.Group.COLUMN_NAME
                    + " from " + DatabaseContract.HasPermission.TABLE_NAME + " hp"
                    + " join " + DatabaseContract.Permission.TABLE_NAME + " p"
                    + " on hp." + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID
                    + " = p." + DatabaseContract.Permission.COLUMN_ID
                    + " join " + DatabaseContract.UserDevice.TABLE_NAME + " u"
                    + " on hp." + DatabaseContract.HasPermission.COLUMN_USER_ID
                    + " = u." + DatabaseContract.UserDevice.COLUMN_ID
                    + " join " + DatabaseContract.Group.TABLE_NAME + " g"
                    + " on u." + DatabaseContract.UserDevice.COLUMN_GROUP_ID
                    + " = g." + DatabaseContract.Group.COLUMN_ID
                    + " join " + DatabaseContract.ElectronicModule.TABLE_NAME + " m"
                    + " on p." + DatabaseContract.Permission.COLUMN_ELECTRONIC_MODULE_ID
                    + " = m." + DatabaseContract.ElectronicModule.COLUMN_ID
                    + " where p." + DatabaseContract.Permission.COLUMN_NAME
                    + " = ? and m." + DatabaseContract.ElectronicModule.COLUMN_NAME
                    + " = ?", new String[] { permission.toString(), moduleName }
            );
        }
        List<UserDevice> userDevices = new LinkedList<>();
        while (userDevicesCursor.moveToNext()) {
            userDevices.add(new UserDevice(userDevicesCursor.getString(0),
                    userDevicesCursor.getString(2), new DeviceID(userDevicesCursor.getString(1))));
        }
        return userDevices;
    }

    /**
     * Get all permissions that a given user device has.
     * @param userDeviceID The UserDevice which has the Permissions which will be returned.
     * @return List of all Permissions that the given UserDevice has.
     */
    public List<PermissionDTO> getPermissionsOfUserDevice(DeviceID userDeviceID) {
        List<PermissionDTO> permissions = new LinkedList<>();
        Cursor permissionCursor;
        permissionCursor = databaseConnector
                .executeSql("select p." + DatabaseContract.Permission.COLUMN_NAME
                        + ", m." + DatabaseContract.ElectronicModule.COLUMN_NAME
                        + " from " + DatabaseContract.HasPermission.TABLE_NAME
                        + " hp join " + DatabaseContract.Permission.TABLE_NAME + " p on "
                        + "hp." + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID
                        + " = p." + DatabaseContract.Permission.COLUMN_ID
                        + " join " + DatabaseContract.UserDevice.TABLE_NAME
                        + " ud on hp." + DatabaseContract.HasPermission.COLUMN_USER_ID
                        + " = ud." + DatabaseContract.UserDevice.COLUMN_ID
                        + " join " + DatabaseContract.ElectronicModule.TABLE_NAME + " m on "
                        + "p." + DatabaseContract.Permission.COLUMN_ELECTRONIC_MODULE_ID + " = "
                        + " m." + DatabaseContract.ElectronicModule.COLUMN_ID
                        + " where ud." + DatabaseContract.UserDevice.COLUMN_FINGERPRINT
                        + " = ?", new String[]{userDeviceID.getIDString()});
        while (permissionCursor.moveToNext()) {
            permissions.add(new PermissionDTO(
                    de.unipassau.isl.evs.ssh.core.sec.Permission.valueOf(permissionCursor.getString(0)),
                    permissionCursor.getString(1)
            ));
        }
        permissionCursor = databaseConnector
                .executeSql("select p." + DatabaseContract.Permission.COLUMN_NAME
                        + " from " + DatabaseContract.HasPermission.TABLE_NAME
                        + " hp join " + DatabaseContract.Permission.TABLE_NAME + " p on "
                        + "hp." + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID
                        + " = p." + DatabaseContract.Permission.COLUMN_ID
                        + " join " + DatabaseContract.UserDevice.TABLE_NAME
                        + " ud on hp." + DatabaseContract.HasPermission.COLUMN_USER_ID
                        + " = ud." + DatabaseContract.UserDevice.COLUMN_ID
                        + " where p." + DatabaseContract.Permission.COLUMN_ELECTRONIC_MODULE_ID
                        + " is NULL and ud." + DatabaseContract.UserDevice.COLUMN_FINGERPRINT
                        + " = ?", new String[]{userDeviceID.getIDString()});
        while (permissionCursor.moveToNext()) {
            permissions.add(new PermissionDTO(de.unipassau.isl.evs.ssh.core.sec.Permission.valueOf(
                    permissionCursor.getString(0)
            )));
        }
        return permissions;
    }
}