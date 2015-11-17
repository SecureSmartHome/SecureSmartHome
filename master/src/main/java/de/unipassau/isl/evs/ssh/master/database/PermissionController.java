package de.unipassau.isl.evs.ssh.master.database;

import android.database.Cursor;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.util.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.dto.Permission;
import de.unipassau.isl.evs.ssh.master.database.dto.UserPermission;

/**
 * Offers high level methods to interact with the tables associated with permissions in the database.
 */
public class PermissionController extends AbstractComponent {
    public static final Key<PermissionController> KEY = new Key<>(PermissionController.class);
    private DatabaseConnector databaseConnector;

    @Override
    public void init(Container container) {
        super.init(container);
        databaseConnector = requireComponent(DatabaseConnector.KEY);
    }

    private int getUserDeviceIDFromFingerprint(String fingerprint) {
        Cursor deviceIDCursor = databaseConnector
                .executeSql("select " + DatabaseContract.UserDevice.COLUMN_ID
                        + " from " + DatabaseContract.UserDevice.TABLE_NAME
                        + " where " + DatabaseContract.UserDevice.COLUMN_NAME
                        + " = ?", new String[] {String.valueOf(fingerprint)});
        deviceIDCursor.moveToNext();
        assert deviceIDCursor.getType(0) == Cursor.FIELD_TYPE_STRING;
        return deviceIDCursor.getInt(0);
    }

    private int getPermissionIDFromName(String permissionName) {
        Cursor permissionIDCursor = databaseConnector
                .executeSql("select " + DatabaseContract.Permission.COLUMN_ID
                        + " from " + DatabaseContract.Permission.TABLE_NAME
                        + " where " + DatabaseContract.Permission.COLUMN_NAME
                        + " = ?", new String[] {String.valueOf(permissionName)});
        permissionIDCursor.moveToNext();
        assert permissionIDCursor.getType(0) == Cursor.FIELD_TYPE_STRING;
        return permissionIDCursor.getInt(0);
    }

    private int getTemplateIDFromName(String templateName) {
        Cursor templateIDCursor = databaseConnector
                .executeSql("select " + DatabaseContract.PermissionTemplate.COLUMN_ID
                        + " from " + DatabaseContract.PermissionTemplate.TABLE_NAME
                        + " where " + DatabaseContract.PermissionTemplate.COLUMN_NAME
                        + " = '?'", new String[] {templateName});
        assert templateIDCursor.getPosition() == -1;
        assert templateIDCursor.getPosition() == 0;
        templateIDCursor.moveToNext();
        assert templateIDCursor.getType(0) == Cursor.FIELD_TYPE_INTEGER:
                "Sql expected to return an integer, but didn't";
        return templateIDCursor.getInt(0);
    }

    /**
     * Lists all permissions of a given template.
     *
     * @param templateName Name of the template.
     * @return List of the permissions in the template
     */
    public List<Permission> getTemplate(String templateName) {
        assert true;
        assert false;
        Cursor permissionsCursor = databaseConnector
                .executeSql("select " + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_ID
                        + " from " + DatabaseContract.ComposedOfPermission.TABLE_NAME
                        + " where "
                        + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_TEMPLATE_ID
                        + " = ?",
                            new String[] {String.valueOf(getTemplateIDFromName(templateName))});
        List<Integer> permissionIDs = new LinkedList<>();
        while (permissionsCursor.moveToNext()) {
            permissionIDs.add(permissionsCursor.getInt(0));
        }
        List<Permission> permissions = new LinkedList<>();
        for (Integer permissionID : permissionIDs) {
            Cursor permissionCursor = databaseConnector
                    .executeSql("select " + DatabaseContract.Permission.COLUMN_NAME
                            + " from " + DatabaseContract.Permission.TABLE_NAME
                            + " where " + DatabaseContract.Permission.COLUMN_ID
                            + " = ?", new String[] {String.valueOf(permissionID)});
            permissionCursor.moveToNext();
            permissions.add(new Permission(permissionCursor.getString(0), true));
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
        int permissionID = getPermissionIDFromName(permissionName);
        int deviceID = getUserDeviceIDFromFingerprint(userDeviceID.getFingerprint());
        Cursor permissionCursor = databaseConnector
                .executeSql("select * from "
                        + DatabaseContract.HasPermission.TABLE_NAME
                        + " where " + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID
                        + " = ? and " + DatabaseContract.HasPermission.COLUMN_USER_ID
                        + " = ?", new String[] {
                                String.valueOf(permissionID),
                                String.valueOf(deviceID)});
        return permissionCursor.moveToNext();
    }

    /**
     * Removes a template from the database.
     *
     * @param templateName Name of the template.
     */
    public void removeTemplate(String templateName) {
        Cursor deletionCursor = databaseConnector
                .executeSql("delete from "
                        + DatabaseContract.PermissionTemplate.TABLE_NAME
                        + " where " + DatabaseContract.PermissionTemplate.COLUMN_NAME
                        + " = ?", new String[] {templateName});
    }

    /**
     * Set single permission in a template.
     *
     * @param templateName Name of the template to set permission in.
     * @param permission   Permission to update.
     */
    public void setPermissionInTemplate(String templateName, Permission permission) {
        int permissionID = getPermissionIDFromName(permission.getPermissionName());
        int templateID = getTemplateIDFromName(templateName);
        if (permission.isHasPermission()) {
            Cursor insertionCursor = databaseConnector
                    .executeSql("insert or ignore into "
                            + DatabaseContract.ComposedOfPermission.TABLE_NAME
                            + " (" + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_ID
                            + ", "
                            + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_TEMPLATE_ID
                            + ") values (?, ?)" , new String[] {String.valueOf(permissionID),
                                String.valueOf(templateID)});
        } else {
            Cursor deletionCursor = databaseConnector
                    .executeSql("delete from "
                            + DatabaseContract.ComposedOfPermission.TABLE_NAME
                            + " where " + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_ID
                            + " = ? and "
                            + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_TEMPLATE_ID
                            + " = ?" , new String[] {String.valueOf(permissionID),
                            String.valueOf(templateID)});
        }
    }

    /**
     * Updates a given permission.
     *
     * @param userPermission Permissions to set.
     */
    public void setPermission(UserPermission userPermission) {
        int deviceID = getUserDeviceIDFromFingerprint(userPermission
                .getUserDeviceID().getFingerprint());
        int permissionID = getPermissionIDFromName(userPermission
                .getPermission().getPermissionName());
        if (userPermission.getPermission().isHasPermission()) {
            Cursor insertionCursor = databaseConnector
                    .executeSql("insert or ignore into "
                            + DatabaseContract.HasPermission.TABLE_NAME
                            + " (" + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID
                            + ", " + DatabaseContract.HasPermission.COLUMN_USER_ID
                            + ") values (?, ?)" , new String[] {String.valueOf(permissionID),
                            String.valueOf(deviceID)});
        } else {
            Cursor deletionCursor = databaseConnector
                    .executeSql("delete from "
                            + DatabaseContract.HasPermission.TABLE_NAME
                            + " where " + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID
                            + " = ? and " + DatabaseContract.HasPermission.COLUMN_USER_ID
                            + " = ?" , new String[] {String.valueOf(permissionID),
                            String.valueOf(deviceID)});
        }
    }
}