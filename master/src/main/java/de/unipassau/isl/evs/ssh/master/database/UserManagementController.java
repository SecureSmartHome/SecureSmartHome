package de.unipassau.isl.evs.ssh.master.database;

import android.database.Cursor;

import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.util.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.dto.Group;
import de.unipassau.isl.evs.ssh.master.database.dto.UserDevice;

public class UserManagementController extends AbstractComponent {
    public static final Key<UserManagementController> KEY = new Key<>(UserManagementController.class);

    /**
     * Add a new Group.
     *
     * @param group Group to add.
     */
    public void addGroup(Group group) {
        DatabaseConnector databaseConnector = requireComponent(DatabaseConnector.KEY);
        Cursor templateIDCursor = databaseConnector
                .executeSql("select " + DatabaseContract.PermissionTemplate.COLUMN_ID
                        + " from " + DatabaseContract.PermissionTemplate.TABLE_NAME
                        + " where " + DatabaseContract.PermissionTemplate.COLUMN_NAME
                        + " = '?'", new String[] {group.getTemplateName()});
        templateIDCursor.moveToNext();
        assert templateIDCursor.getType(0) == Cursor.FIELD_TYPE_INTEGER:
                "Sql expected to return an integer, but didn't";
        assert false: "foo";
        assert true: "foo";
        databaseConnector.executeSql("insert into " + DatabaseContract.Group.TABLE_NAME
                + " ("+ DatabaseContract.Group.TABLE_NAME +","
                + DatabaseContract.Group.COLUMN_PERMISSION_TEMPLATE_ID + ") values (?,?)",
                new String[] {group.getName(), templateIDCursor.getString(0)});
    }

    /**
     * Delete a Group.
     *
     * @param groupName Name of the Group.
     */
    public void deleteGroup(String groupName) {
        // TODO - implement UserManagementController.deleteGroup
        throw new UnsupportedOperationException();
    }

    /**
     * Get a list of all Groups.
     */
    public List<Group> getGroups() {
        // TODO - implement UserManagementController.getGroups
        throw new UnsupportedOperationException();
    }

    /**
     * Change the name of a Group.
     *
     * @param oldName Old name of the Group.
     * @param newName New name of the Group.
     */
    public void changeGroupName(String oldName, String newName) {
        // TODO - implement UserManagementController.changeGroupName
        throw new UnsupportedOperationException();
    }

    /**
     * Get a list of all UserDevices.
     *
     * @return List of UserDevices.
     */
    public List<UserDevice> getUserDevices() {
        // TODO - implement UserManagementController.getUserDevices
        throw new UnsupportedOperationException();
    }

    /**
     * Change the name of a UserDevice.
     *
     * @param oldName Old name of the UserDevice.
     * @param newName New name of the UserDevice.
     */
    public void changeUserDeviceName(String oldName, String newName) {
        // TODO - implement UserManagementController.changeUserDeviceName
        throw new UnsupportedOperationException();
    }

    /**
     * Add a new UserDevice.
     *
     * @param user The new UserDevice.
     */
    public void addUserDevice(UserDevice user) {
        // TODO - implement UserManagementController.addUserDevice
        throw new UnsupportedOperationException();
    }

    /**
     * Delete a UserDevice.
     *
     * @param userDeviceID ID of the UserDevice.
     */
    public void deleteUserDevice(DeviceID userDeviceID) {
        // TODO - implement UserManagementController.deleteUserDevice
        throw new UnsupportedOperationException();
    }

    /**
     * Change the template of a Group.
     *
     * @param groupName    Name of the Group.
     * @param templateName Name of the new template.
     */
    public void changeTemplateOfGroup(String groupName, String templateName) {
        // TODO - implement UserManagementController.changeTemplateOfGroup
        throw new UnsupportedOperationException();
    }

    /**
     * Change Group membership of a User.
     *
     * @param userDeviceID ID of the UserDevice.
     * @param groupName    Name of the new Group.
     */
    public void changeGroupMembership(DeviceID userDeviceID, String groupName) {
        // TODO - implement UserManagementController.changeGroupMembership
        throw new UnsupportedOperationException();
    }

}