package de.unipassau.isl.evs.ssh.master.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.dto.Group;
import de.unipassau.isl.evs.ssh.master.database.dto.UserDevice;

public class UserManagementController extends AbstractComponent {
    public static final Key<UserManagementController> KEY = new Key<>(UserManagementController.class);
    private DatabaseConnector databaseConnector;
    private static final String TEMPLATE_ID_FROM_NAME_SQL_QUERY =
                        "select " + DatabaseContract.PermissionTemplate.COLUMN_ID
                        + " from " + DatabaseContract.PermissionTemplate.TABLE_NAME
                        + " where " + DatabaseContract.PermissionTemplate.COLUMN_NAME
                        + " = ?";
    private static final String GROUP_ID_FROM_NAME_SQL_QUERY =
                        "select " + DatabaseContract.Group.COLUMN_ID
                                + " from " + DatabaseContract.Group.TABLE_NAME
                                + " where " + DatabaseContract.Group.COLUMN_NAME
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
     * Add a new Group.
     *
     * @param group Group to add.
     */
    public void addGroup(Group group) {
        databaseConnector.executeSql("insert or ignore into "
                        + DatabaseContract.Group.TABLE_NAME
                        + " ("+ DatabaseContract.Group.COLUMN_NAME + ","
                        + DatabaseContract.Group.COLUMN_PERMISSION_TEMPLATE_ID + ") values (?,("
                        + TEMPLATE_ID_FROM_NAME_SQL_QUERY + "))",
                            new String[] { group.getName(), group.getTemplateName() });
    }

    /**
     * Delete a Group.
     *
     * @param groupName Name of the Group.
     */
    public void removeGroup(String groupName) throws InUseException {
        try {
            databaseConnector.executeSql("delete from "
                            + DatabaseContract.Group.TABLE_NAME
                            + " where " + DatabaseContract.Group.COLUMN_NAME + " = ?",
                    new String[] { groupName });
        } catch (SQLiteConstraintException sqlce) {
            throw new InUseException("This group is used by at least one UserDevice");
        }
    }

    /**
     * Get a list of all Groups.
     */
    public List<Group> getGroups() {
        Cursor groupsCursor = databaseConnector.executeSql("select g."
                + DatabaseContract.Group.COLUMN_NAME
                + ", t." + DatabaseContract.PermissionTemplate.COLUMN_NAME
                + " from " + DatabaseContract.Group.TABLE_NAME + " g"
                + " join " + DatabaseContract.PermissionTemplate.TABLE_NAME + " t"
                + " on g." + DatabaseContract.Group.COLUMN_PERMISSION_TEMPLATE_ID + " = t."
                + DatabaseContract.PermissionTemplate.COLUMN_ID, new String[]{});
        List<Group> groups = new LinkedList<>();
        while (groupsCursor.moveToNext()) {
            groups.add(new Group(groupsCursor.getString(0), groupsCursor.getString(1)));
        }
        return groups;
    }

    /**
     * Change the name of a Group.
     *
     * @param oldName Old name of the Group.
     * @param newName New name of the Group.
     */
    public void changeGroupName(String oldName, String newName) {
        databaseConnector.executeSql("update or ignore " + DatabaseContract.Group.TABLE_NAME
                + " set " + DatabaseContract.Group.COLUMN_NAME
                + " = ? where " + DatabaseContract.Group.COLUMN_NAME + " = ?",
                    new String[] { newName, oldName });
    }

    /**
     * Get a list of all UserDevices.
     *
     * @return List of UserDevices.
     */
    public List<UserDevice> getUserDevices() {
        Cursor userDevicesCursor = databaseConnector.executeSql("select u."
                + DatabaseContract.UserDevice.COLUMN_NAME
                + ", u." + DatabaseContract.UserDevice.COLUMN_FINGERPRINT
                + ", g." + DatabaseContract.Group.COLUMN_NAME
                + " from " + DatabaseContract.UserDevice.TABLE_NAME + " u"
                + " join " + DatabaseContract.Group.TABLE_NAME + " g"
                + " on u." + DatabaseContract.UserDevice.COLUMN_GROUP_ID + " = g."
                + DatabaseContract.Group.COLUMN_ID, new String[] {});
        List<UserDevice> userDevices = new LinkedList<>();
        while (userDevicesCursor.moveToNext()) {
            userDevices.add(new UserDevice(userDevicesCursor.getString(0),
                    userDevicesCursor.getString(2), new DeviceID(userDevicesCursor.getString(1))));
        }
        return userDevices;
    }

    /**
     * Change the name of a UserDevice.
     *
     * @param oldName Old name of the UserDevice.
     * @param newName New name of the UserDevice.
     */
    public void changeUserDeviceName(String oldName, String newName) {
        databaseConnector.executeSql("update or ignore " + DatabaseContract.UserDevice.TABLE_NAME
                        + " set " + DatabaseContract.UserDevice.COLUMN_NAME
                        + " = ? where " + DatabaseContract.UserDevice.COLUMN_NAME + " = ?",
                new String[] { newName, oldName });
    }

    /**
     * Add a new UserDevice.
     *
     * @param user The new UserDevice.
     */
    public void addUserDevice(UserDevice user) {
        databaseConnector.executeSql("insert or ignore into "
                        + DatabaseContract.UserDevice.TABLE_NAME
                        + " ("+ DatabaseContract.UserDevice.COLUMN_NAME + ","
                        + DatabaseContract.UserDevice.COLUMN_FINGERPRINT + ","
                        + DatabaseContract.UserDevice.COLUMN_GROUP_ID + ") values (?, ?,("
                        + GROUP_ID_FROM_NAME_SQL_QUERY + "))",
                new String[] { user.getName(), user.getUserDeviceID().getFingerprint(),
                        user.getInGroup() });
    }

    /**
     * Delete a UserDevice.
     *
     * @param userDeviceID ID of the UserDevice.
     */
    public void removeUserDevice(DeviceID userDeviceID) {
        databaseConnector.executeSql("delete from "
                        + DatabaseContract.UserDevice.TABLE_NAME
                        + " where " + DatabaseContract.UserDevice.COLUMN_FINGERPRINT + " = ?",
                            new String[] { userDeviceID.getFingerprint() });
    }

    /**
     * Change the template of a Group.
     *
     * @param groupName    Name of the Group.
     * @param templateName Name of the new template.
     */
    public void changeTemplateOfGroup(String groupName, String templateName) {
        databaseConnector.executeSql("update " + DatabaseContract.Group.TABLE_NAME
                        + " set " + DatabaseContract.Group.COLUMN_PERMISSION_TEMPLATE_ID
                        + " = (" + TEMPLATE_ID_FROM_NAME_SQL_QUERY
                        + ") where " + DatabaseContract.Group.COLUMN_NAME + " = ?",
                new String[] { templateName, groupName });
    }

    /**
     * Change Group membership of a User.
     *
     * @param userDeviceID ID of the UserDevice.
     * @param groupName    Name of the new Group.
     */
    public void changeGroupMembership(DeviceID userDeviceID, String groupName) {
        databaseConnector.executeSql("update or ignore " + DatabaseContract.UserDevice.TABLE_NAME
                        + " set " + DatabaseContract.UserDevice.COLUMN_GROUP_ID
                        + " = (" + GROUP_ID_FROM_NAME_SQL_QUERY
                        + ") where " + DatabaseContract.UserDevice.COLUMN_FINGERPRINT + " = ?",
                new String[] { groupName, userDeviceID.getFingerprint() });
    }

    public Group getGroup(String groupName) {
        Cursor groupCursor = databaseConnector.executeSql("select g."
                + DatabaseContract.Group.COLUMN_NAME
                + ", t." + DatabaseContract.PermissionTemplate.COLUMN_NAME
                + " from " + DatabaseContract.Group.TABLE_NAME + " g"
                + " join " + DatabaseContract.PermissionTemplate.TABLE_NAME + " t"
                + " on g." + DatabaseContract.Group.COLUMN_PERMISSION_TEMPLATE_ID + " = t."
                + DatabaseContract.PermissionTemplate.COLUMN_ID
                + " where g." + DatabaseContract.Group.COLUMN_NAME + " = ?",
                    new String[] { groupName });
        if (groupCursor.moveToNext()) {
            return new Group(groupCursor.getString(0), groupCursor.getString(1));
        }
        return null;
    }

    public UserDevice getUserDevice(DeviceID deviceID) {
        Cursor userDeviceCursor = databaseConnector.executeSql("select u."
                + DatabaseContract.UserDevice.COLUMN_NAME
                + ", u." + DatabaseContract.UserDevice.COLUMN_FINGERPRINT
                + ", g." + DatabaseContract.Group.COLUMN_NAME
                + " from " + DatabaseContract.UserDevice.TABLE_NAME + " u"
                + " join " + DatabaseContract.Group.TABLE_NAME + " g"
                + " on u." + DatabaseContract.UserDevice.COLUMN_GROUP_ID + " = g."
                + DatabaseContract.Group.COLUMN_ID
                + " where " + DatabaseContract.UserDevice.COLUMN_FINGERPRINT + " = ?",
                    new String[] { deviceID.getFingerprint() });
        if (userDeviceCursor.moveToNext()) {
            return new UserDevice(userDeviceCursor.getString(0),
                    userDeviceCursor.getString(2), new DeviceID(userDeviceCursor.getString(1)));
        }
        return null;
    }

}