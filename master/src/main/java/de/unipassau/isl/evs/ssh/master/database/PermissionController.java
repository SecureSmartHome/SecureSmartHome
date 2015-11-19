package de.unipassau.isl.evs.ssh.master.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * Offers high level methods to interact with the tables associated with permissions in the database.
 */
public class PermissionController extends AbstractComponent {
    public static final Key<PermissionController> KEY = new Key<>(PermissionController.class);
    private DatabaseConnector databaseConnector;
    private static final String PERMISSION_ID_FROM_NAME_SQL_QUERY =
                        "select " + DatabaseContract.Permission.COLUMN_ID
                        + " from " + DatabaseContract.Permission.TABLE_NAME
                        + " where " + DatabaseContract.Permission.COLUMN_NAME
                        + " = ?";
    private static final String TEMPLATE_ID_FROM_NAME_SQL_QUERY =
                        "select " + DatabaseContract.PermissionTemplate.COLUMN_ID
                        + " from " + DatabaseContract.PermissionTemplate.TABLE_NAME
                        + " where " + DatabaseContract.PermissionTemplate.COLUMN_NAME
                        + " = ?";
    private static final String USER_DEVICE_ID_FROM_FINGERPRINT_SQL_QUERY =
                        "select " + DatabaseContract.UserDevice.COLUMN_ID
                        + " from " + DatabaseContract.UserDevice.TABLE_NAME
                        + " where " + DatabaseContract.UserDevice.COLUMN_FINGERPRINT
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
     * Lists all permissions of a given template.
     *
     * @param templateName Name of the template.
     * @return List of the permissions in the template
     */
    public List<String> getPermissionsOfTemplate(String templateName) {
        Cursor permissionsCursor = databaseConnector
                .executeSql("select p." + DatabaseContract.Permission.COLUMN_NAME
                        + " from " + DatabaseContract.ComposedOfPermission.TABLE_NAME + " comp "
                        + "join " + DatabaseContract.PermissionTemplate.TABLE_NAME + " pt on comp."
                        + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_TEMPLATE_ID
                        + " = pt." + DatabaseContract.PermissionTemplate.COLUMN_ID
                        + " join " + DatabaseContract.Permission.TABLE_NAME
                        + " p on comp." + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_ID
                        + " = p." + DatabaseContract.Permission.COLUMN_ID
                        + " where pt." + DatabaseContract.PermissionTemplate.COLUMN_NAME
                        + " = ?", new String[] { templateName });
        List<String> permissions = new LinkedList<>();
        while (permissionsCursor.moveToNext()) {
            permissions.add(permissionsCursor.getString(0));
        }
        return permissions;
    }

    /**
     * Returns whether a given user has a given permission.
     *
     * @param userDeviceID   DeviceID associated with the user.
     * @param permissionName Name of the permission.
     * @return true if has permissions otherwise false.
     */
    public boolean hasPermission(DeviceID userDeviceID, String permissionName) {
        Cursor permissionCursor = databaseConnector
                .executeSql("select * from " + DatabaseContract.HasPermission.TABLE_NAME
                        + " hp join " + DatabaseContract.Permission.TABLE_NAME + " p on "
                        + "hp." + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID
                        + " = p." + DatabaseContract.Permission.COLUMN_ID
                        + " join " + DatabaseContract.UserDevice.TABLE_NAME
                        + " ud on hp." + DatabaseContract.HasPermission.COLUMN_USER_ID
                        + " = ud." + DatabaseContract.UserDevice.COLUMN_ID
                        + " where p." + DatabaseContract.Permission.COLUMN_NAME
                        + " = ? and ud." + DatabaseContract.UserDevice.COLUMN_FINGERPRINT
                        + " = ?", new String[] { permissionName, userDeviceID.getFingerprint()});
        return permissionCursor.moveToNext();
    }

    /**
     * Removes a template from the database.
     *
     * @param templateName Name of the template.
     */
    public void removeTemplate(String templateName) throws InUseException {
        try {
            databaseConnector.executeSql("delete from "
                            + DatabaseContract.PermissionTemplate.TABLE_NAME
                            + " where " + DatabaseContract.PermissionTemplate.COLUMN_NAME
                            + " = ?", new String[] { templateName });
        } catch (SQLiteConstraintException sqlce) {
            throw new InUseException("This template is used by at least one Group");
        }
    }

    public void addPermissionToTemplate(String templateName, String permissionName) throws IllegalReferenceException {
        try {
            databaseConnector.executeSql("insert into "
                            + DatabaseContract.ComposedOfPermission.TABLE_NAME
                            + " (" + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_ID
                            + ", "
                            + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_TEMPLATE_ID
                            + ") values ((" + PERMISSION_ID_FROM_NAME_SQL_QUERY
                            + "), (" + TEMPLATE_ID_FROM_NAME_SQL_QUERY + "))",
                    new String[] { permissionName, templateName} );
        } catch (SQLiteConstraintException sqlce) {
            throw new IllegalReferenceException(
                    "The given Template or Permission does not exist in the database");
        }
    }

    //todo: check
    public void removePermissionFromTemplate(String templateName, String permissionName) {
        databaseConnector.executeSql("delete from "
                        + DatabaseContract.ComposedOfPermission.TABLE_NAME
                        + " where " + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_ID
                        + " = (" + PERMISSION_ID_FROM_NAME_SQL_QUERY + ") and "
                        + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_TEMPLATE_ID
                        + " = (" + TEMPLATE_ID_FROM_NAME_SQL_QUERY + ")" ,
                        new String[] { permissionName, templateName });
    }

    public void addUserPermission(DeviceID userDeviceID, String permissionName) throws IllegalReferenceException {
        try {
            databaseConnector.executeSql("insert into "
                    + DatabaseContract.HasPermission.TABLE_NAME
                    + " (" + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID
                    + ", " + DatabaseContract.HasPermission.COLUMN_USER_ID
                    + ") values ((" + PERMISSION_ID_FROM_NAME_SQL_QUERY
                    + "), (" + USER_DEVICE_ID_FROM_FINGERPRINT_SQL_QUERY
                    + "))", new String[]{permissionName,
                    userDeviceID.getFingerprint()});
        } catch (SQLiteConstraintException sqlce) {
            throw new IllegalReferenceException(
                    "The given UserDevice or Permission does not exist in the database");
        }
    }

    //todo: check
    public void removeUserPermission(DeviceID userDeviceID, String permissionName) {
        databaseConnector.executeSql("delete from "
                + DatabaseContract.HasPermission.TABLE_NAME
                + " where " + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID
                + " = (" + PERMISSION_ID_FROM_NAME_SQL_QUERY
                + ") and " + DatabaseContract.HasPermission.COLUMN_USER_ID
                + " = (" + USER_DEVICE_ID_FROM_FINGERPRINT_SQL_QUERY
                +")" , new String[] { permissionName, userDeviceID.getFingerprint() });
    }


    /**
     * Adds a new template to the database.
     *
     * @param templateName Name of the template.
     */
    public void addTemplate(String templateName) {
        databaseConnector.executeSql("insert or ignore into "
                        + DatabaseContract.PermissionTemplate.TABLE_NAME
                        + " (" + DatabaseContract.PermissionTemplate.COLUMN_NAME + ")"
                        + "values (?)", new String[] { templateName });
    }

    /**
     * Adds a new permission to the database.
     *
     * @param permissionName Name of the permission.
     */
    public void addPermission(String permissionName) {
        databaseConnector.executeSql("insert or ignore into "
                        + DatabaseContract.Permission.TABLE_NAME
                        + " (" + DatabaseContract.Permission.COLUMN_NAME + ")"
                        + "values (?)", new String[]{permissionName});
    }

    /**
     * Removes a permission from the database.
     *
     * @param permissionName Name of the permission.
     */
    public void removePermission(String permissionName) {
        databaseConnector.executeSql("delete from "
                        + DatabaseContract.Permission.TABLE_NAME
                        + " where " + DatabaseContract.Permission.COLUMN_NAME
                        + " = ?", new String[]{permissionName});
    }

    public List<String> getPermissions() {
        Cursor permissionsCursor = databaseConnector.executeSql("select "
                + DatabaseContract.Permission.COLUMN_NAME
                + " from " + DatabaseContract.Permission.TABLE_NAME, new String[] {});
        List<String> permissions = new LinkedList<>();
        while (permissionsCursor.moveToNext()) {
            permissions.add(permissionsCursor.getString(0));
        }
        return permissions;
    }

    public List<String> getTemplates() {
        Cursor templatesCursor = databaseConnector.executeSql("select "
                + DatabaseContract.PermissionTemplate.COLUMN_NAME
                + " from " + DatabaseContract.PermissionTemplate.TABLE_NAME, new String[] {});
        List<String> templates = new LinkedList<>();
        while (templatesCursor.moveToNext()) {
            templates.add(templatesCursor.getString(0));
        }
        return templates;
    }
}