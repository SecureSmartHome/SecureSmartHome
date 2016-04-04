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
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.AlreadyInUseException;
import de.unipassau.isl.evs.ssh.core.database.DatabaseControllerException;
import de.unipassau.isl.evs.ssh.core.database.IsReferencedException;
import de.unipassau.isl.evs.ssh.core.database.UnknownReferenceException;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * Offers high level methods to interact with the tables associated with users and groups in the database.
 *
 * @author Leon Sell
 */
public class UserManagementController extends AbstractComponent {
    public static final Key<UserManagementController> KEY = new Key<>(UserManagementController.class);
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
     * Add a new Group.
     *
     * @param group Group to add.
     */
    public void addGroup(Group group) throws DatabaseControllerException {
        try {
            databaseConnector.executeSql("insert into "
                            + DatabaseContract.Group.TABLE_NAME
                            + " (" + DatabaseContract.Group.COLUMN_NAME + ","
                            + DatabaseContract.Group.COLUMN_PERMISSION_TEMPLATE_ID + ") values (?,("
                            + DatabaseContract.SqlQueries.TEMPLATE_ID_FROM_NAME_SQL_QUERY + "))",
                    new String[]{group.getName(), group.getTemplateName()});
        } catch (SQLiteConstraintException sqlce) {
            throw new DatabaseControllerException("Either the given Template does not exist in the database"
                    + "or the name is already in use by another Group.", sqlce);
        }
    }

    /**
     * Delete a Group.
     *
     * @param groupName Name of the Group.
     */
    public void removeGroup(String groupName) throws IsReferencedException {
        try {
            databaseConnector.executeSql("delete from "
                            + DatabaseContract.Group.TABLE_NAME
                            + " where " + DatabaseContract.Group.COLUMN_NAME + " = ?",
                    new String[]{groupName});
        } catch (SQLiteConstraintException sqlce) {
            throw new IsReferencedException("This group is used by at least one UserDevice", sqlce);
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
    public void changeGroupName(String oldName, String newName) throws AlreadyInUseException {
        try {
            databaseConnector.executeSql("update " + DatabaseContract.Group.TABLE_NAME
                            + " set " + DatabaseContract.Group.COLUMN_NAME
                            + " = ? where " + DatabaseContract.Group.COLUMN_NAME + " = ?",
                    new String[]{newName, oldName});
        } catch (SQLiteConstraintException sqlce) {
            throw new AlreadyInUseException("The given name is already used by another Group.", sqlce);
        }
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
                + DatabaseContract.Group.COLUMN_ID, new String[]{});
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
     * @param deviceID ID of the device.
     * @param newName  New name of the UserDevice.
     */
    public void changeUserDeviceName(DeviceID deviceID, String newName) throws AlreadyInUseException {
        try {
            databaseConnector.executeSql("update "
                            + DatabaseContract.UserDevice.TABLE_NAME
                            + " set " + DatabaseContract.UserDevice.COLUMN_NAME
                            + " = ? where " + DatabaseContract.UserDevice.COLUMN_FINGERPRINT + " = ?",
                    new String[]{newName, deviceID.getIDString()});
        } catch (SQLiteConstraintException sqlce) {
            throw new AlreadyInUseException("The given name is already used by another UserDevice.", sqlce);
        }
    }

    /**
     * Add a new UserDevice.
     *
     * @param userDevice The new UserDevice.
     */
    public void addUserDevice(UserDevice userDevice) throws DatabaseControllerException {
        try {
            databaseConnector.executeSql("insert into "
                            + DatabaseContract.UserDevice.TABLE_NAME
                            + " (" + DatabaseContract.UserDevice.COLUMN_NAME + ","
                            + DatabaseContract.UserDevice.COLUMN_FINGERPRINT + ","
                            + DatabaseContract.UserDevice.COLUMN_GROUP_ID + ") values (?, ?,("
                            + DatabaseContract.SqlQueries.GROUP_ID_FROM_NAME_SQL_QUERY + "))",
                    new String[]{userDevice.getName(), userDevice.getUserDeviceID().getIDString(),
                            userDevice.getInGroup()});
        } catch (SQLiteConstraintException sqlce) {
            throw new DatabaseControllerException(
                    "Either the given Group does not exist in the database"
                            + " or a UserDevice already has the given name or fingerprint.", sqlce);
        }
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
                new String[]{userDeviceID.getIDString()});
    }

    /**
     * Change the template of a Group.
     *
     * @param groupName    Name of the Group.
     * @param templateName Name of the new template.
     */
    public void changeTemplateOfGroup(String groupName, String templateName) throws UnknownReferenceException {
        try {
            databaseConnector.executeSql("update " + DatabaseContract.Group.TABLE_NAME
                            + " set " + DatabaseContract.Group.COLUMN_PERMISSION_TEMPLATE_ID
                            + " = (" + DatabaseContract.SqlQueries.TEMPLATE_ID_FROM_NAME_SQL_QUERY
                            + ") where " + DatabaseContract.Group.COLUMN_NAME + " = ?",
                    new String[]{templateName, groupName});
        } catch (SQLiteConstraintException sqlce) {
            throw new UnknownReferenceException("The given Template does not exist in the database", sqlce);
        }
    }

    /**
     * Change Group membership of a User.
     *
     * @param userDeviceID ID of the UserDevice.
     * @param groupName    Name of the new Group.
     */
    public void changeGroupMembership(DeviceID userDeviceID, String groupName) throws UnknownReferenceException {
        try {
            databaseConnector.executeSql("update " + DatabaseContract.UserDevice.TABLE_NAME
                            + " set " + DatabaseContract.UserDevice.COLUMN_GROUP_ID
                            + " = (" + DatabaseContract.SqlQueries.GROUP_ID_FROM_NAME_SQL_QUERY
                            + ") where " + DatabaseContract.UserDevice.COLUMN_FINGERPRINT + " = ?",
                    new String[]{groupName, userDeviceID.getIDString()});
        } catch (SQLiteConstraintException sqlce) {
            throw new UnknownReferenceException("The given Group does not exist in the database", sqlce);
        }
    }

    /**
     * Get a single Group by name.
     *
     * @param groupName Name of the Group.
     * @return The requested Group.
     */
    public Group getGroup(String groupName) {
        Cursor groupCursor = databaseConnector.executeSql("select g."
                        + DatabaseContract.Group.COLUMN_NAME
                        + ", t." + DatabaseContract.PermissionTemplate.COLUMN_NAME
                        + " from " + DatabaseContract.Group.TABLE_NAME + " g"
                        + " join " + DatabaseContract.PermissionTemplate.TABLE_NAME + " t"
                        + " on g." + DatabaseContract.Group.COLUMN_PERMISSION_TEMPLATE_ID + " = t."
                        + DatabaseContract.PermissionTemplate.COLUMN_ID
                        + " where g." + DatabaseContract.Group.COLUMN_NAME + " = ?",
                new String[]{groupName});
        if (groupCursor.moveToNext()) {
            return new Group(groupCursor.getString(0), groupCursor.getString(1));
        }
        return null;
    }

    /**
     * Get a UserDevice by DeviceID.
     *
     * @param deviceID DeviceID of the UserDevice.
     * @return The requested UserDevice.
     */
    public UserDevice getUserDevice(DeviceID deviceID) {
        if (deviceID == null || Strings.isNullOrEmpty(deviceID.getIDString())) {
            return null;
        }
        Cursor userDeviceCursor = databaseConnector.executeSql("select u."
                        + DatabaseContract.UserDevice.COLUMN_NAME
                        + ", u." + DatabaseContract.UserDevice.COLUMN_FINGERPRINT
                        + ", g." + DatabaseContract.Group.COLUMN_NAME
                        + " from " + DatabaseContract.UserDevice.TABLE_NAME + " u"
                        + " join " + DatabaseContract.Group.TABLE_NAME + " g"
                        + " on u." + DatabaseContract.UserDevice.COLUMN_GROUP_ID + " = g."
                        + DatabaseContract.Group.COLUMN_ID
                        + " where u." + DatabaseContract.UserDevice.COLUMN_FINGERPRINT + " = ?",
                new String[]{deviceID.getIDString()});
        if (userDeviceCursor.moveToNext()) {
            return new UserDevice(userDeviceCursor.getString(0),
                    userDeviceCursor.getString(2), new DeviceID(userDeviceCursor.getString(1)));
        }
        return null;
    }

    /**
     * Get a UserDevice by Name
     *
     * @param name Name of the UserDevice.
     * @return The requested UserDevice.
     */
    public UserDevice getUserDevice(String name) {
        if (Strings.isNullOrEmpty(name)) {
            return null;
        }
        Cursor userDeviceCursor = databaseConnector.executeSql("select u."
                        + DatabaseContract.UserDevice.COLUMN_NAME
                        + ", u." + DatabaseContract.UserDevice.COLUMN_FINGERPRINT
                        + ", g." + DatabaseContract.Group.COLUMN_NAME
                        + " from " + DatabaseContract.UserDevice.TABLE_NAME + " u"
                        + " join " + DatabaseContract.Group.TABLE_NAME + " g"
                        + " on u." + DatabaseContract.UserDevice.COLUMN_GROUP_ID + " = g."
                        + DatabaseContract.Group.COLUMN_ID
                        + " where u." + DatabaseContract.UserDevice.COLUMN_NAME + " = ?",
                new String[]{name});
        if (userDeviceCursor.moveToNext()) {
            return new UserDevice(userDeviceCursor.getString(0),
                    userDeviceCursor.getString(2), new DeviceID(userDeviceCursor.getString(1)));
        }
        return null;
    }
}