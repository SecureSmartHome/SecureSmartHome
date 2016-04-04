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

import android.content.Context;
import android.test.InstrumentationTestCase;

import junit.framework.Assert;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;
import de.unipassau.isl.evs.ssh.core.database.AlreadyInUseException;
import de.unipassau.isl.evs.ssh.core.database.DatabaseControllerException;
import de.unipassau.isl.evs.ssh.core.database.IsReferencedException;
import de.unipassau.isl.evs.ssh.core.database.UnknownReferenceException;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.HolidayAction;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.USBAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.PermissionDTO;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * Test class for all controller functionality (database interface)
 *
 * @author Leon Sell
 */
public class ControllerTest extends InstrumentationTestCase {
    private final DeviceID device1 = new DeviceID("YXNqZGZsw7ZrYXNkZmFhc2RmdmF3YXZhc3Zhc3ZhZmE=");
    private final DeviceID device2 = new DeviceID("YXNqZGZsw7ZiYXNkZmFhc2RmdmF3YXZhc3Zhc3ZhZmE=");
    private final DeviceID device3 = new DeviceID("YXNqZGZsw7ZiYXNkZmFhc2RmdmltZHZhc3Zhc3ZhZmE=");
    private final DeviceID device4 = new DeviceID("YXNqZGZsw7ZiYXMnK2Fhc2RmdmltZHZhc3Zhc3ZhZmE=");
    private final DeviceID altDevice1 = new DeviceID("YXNqZGZsw7ZiYXMnK2Fhc2RmdmltZHZhcz0/c3ZhZmE=");
    private final DeviceID altDevice2 = new DeviceID("YXNqZGZsw7ZiYXMnK2Fhc2RmdUltZHZhcz0/c3ZhZmE=");

    public void testTemplatesAndPermissions() throws DatabaseControllerException {
        Context context = getInstrumentation().getTargetContext();
        //Clear database before running tests to assure clean test
        context.deleteDatabase(DatabaseConnector.DATABASE_NAME);
        SimpleContainer container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(context));
        container.register(DatabaseConnector.KEY, new DatabaseConnector());
        container.register(PermissionController.KEY, new PermissionController());
        container.register(SlaveController.KEY, new SlaveController());
        PermissionController permissionController = container.require(PermissionController.KEY);
        SlaveController slaveController = container.require(SlaveController.KEY);

        //Test Templates w/ permissions
        //Add Templates
        permissionController.addTemplate("Whaddaup");
        permissionController.addTemplate("asdf");

        //Modules w/ Slaves to test Permissions
        slaveController.addSlave(new Slave("s1", device1, null));
        slaveController.addModule(new Module("m1", device1, CoreConstants.ModuleType.Light,
                new USBAccessPoint(1)));

        //Add Permissions
        permissionController.addPermission(de.unipassau.isl.evs.ssh.core.sec.Permission.DELETE_ODROID, null);
        permissionController.addPermission(de.unipassau.isl.evs.ssh.core.sec.Permission.BELL_RANG, null);
        permissionController.addPermission(de.unipassau.isl.evs.ssh.core.sec.Permission.REQUEST_LIGHT_STATUS, null);
        permissionController.addPermission(de.unipassau.isl.evs.ssh.core.sec.Permission.SWITCH_LIGHT, "m1");

        //Add same name. Will not fail when module is null for both and name is same.
        try {
            permissionController.addPermission(de.unipassau.isl.evs.ssh.core.sec.Permission.SWITCH_LIGHT, "m1");
            Assert.fail("PermissionDTO controller should have thrown AlreadyInUseException");
        } catch (DatabaseControllerException e) {
        }

        //Check that permissions are inserted
        List<de.unipassau.isl.evs.ssh.core.sec.Permission> permissions = new LinkedList<>();
        for (PermissionDTO permission : permissionController.getPermissions()) {
            permissions.add(permission.getPermission());
        }
        assertTrue(permissions.containsAll(Arrays.asList(
                de.unipassau.isl.evs.ssh.core.sec.Permission.DELETE_ODROID,
                de.unipassau.isl.evs.ssh.core.sec.Permission.BELL_RANG,
                de.unipassau.isl.evs.ssh.core.sec.Permission.REQUEST_LIGHT_STATUS,
                de.unipassau.isl.evs.ssh.core.sec.Permission.SWITCH_LIGHT
        )));

        //Remove PermissionDTO
        permissionController.removePermission(de.unipassau.isl.evs.ssh.core.sec.Permission.SWITCH_LIGHT, "m1");

        //Check that permission is removed
        permissions = new LinkedList<>();
        for (PermissionDTO permission : permissionController.getPermissions()) {
            permissions.add(permission.getPermission());
        }
        assertFalse(permissions.contains(de.unipassau.isl.evs.ssh.core.sec.Permission.SWITCH_LIGHT));

        //Remove element that is already removed
        permissionController.removePermission(de.unipassau.isl.evs.ssh.core.sec.Permission.SWITCH_LIGHT, "m1");

        //Check that it's still not in database and not getting added again magically ~~~
        permissions = new LinkedList<>();
        for (PermissionDTO permission : permissionController.getPermissions()) {
            permissions.add(permission.getPermission());
        }
        assertFalse(permissions.contains(de.unipassau.isl.evs.ssh.core.sec.Permission.SWITCH_LIGHT));

        //Add permissions to templates.
        permissionController.addPermissionToTemplate("Whaddaup", de.unipassau.isl.evs.ssh.core.sec.Permission.DELETE_ODROID, null);
        permissionController.addPermissionToTemplate("Whaddaup", de.unipassau.isl.evs.ssh.core.sec.Permission.BELL_RANG, null);
        permissionController.addPermissionToTemplate("Whaddaup", de.unipassau.isl.evs.ssh.core.sec.Permission.REQUEST_LIGHT_STATUS, null);
        permissionController.addPermissionToTemplate("asdf", de.unipassau.isl.evs.ssh.core.sec.Permission.DELETE_ODROID, null);

        try {
            permissionController.addPermissionToTemplate("44", de.unipassau.isl.evs.ssh.core.sec.Permission.DELETE_ODROID, null);
            Assert.fail("PermissionDTO controller should have thrown UnknownReferenceException");
        } catch (UnknownReferenceException unknownReferenceException) {
        }

        //Check that permissions are in template
        permissions = new LinkedList<>();
        for (PermissionDTO permission : permissionController.getPermissionsOfTemplate("Whaddaup")) {
            permissions.add(permission.getPermission());
        }
        assertTrue(permissions.containsAll(Arrays.asList(
                de.unipassau.isl.evs.ssh.core.sec.Permission.DELETE_ODROID,
                de.unipassau.isl.evs.ssh.core.sec.Permission.BELL_RANG,
                de.unipassau.isl.evs.ssh.core.sec.Permission.REQUEST_LIGHT_STATUS
                )));

        //Check that permissions are in template
        permissions = new LinkedList<>();
        for (PermissionDTO permission : permissionController.getPermissionsOfTemplate("asdf")) {
            permissions.add(permission.getPermission());
        }
        assertTrue(permissions.contains(de.unipassau.isl.evs.ssh.core.sec.Permission.DELETE_ODROID));

        //Remove permissions from template
        permissionController.removePermissionFromTemplate("Whaddaup", de.unipassau.isl.evs.ssh.core.sec.Permission.REQUEST_LIGHT_STATUS, null);

        //Check that template and permission removed from template still exist
        permissions = new LinkedList<>();
        for (PermissionDTO permission : permissionController.getPermissions()) {
            permissions.add(permission.getPermission());
        }

        assertTrue(permissions.contains(de.unipassau.isl.evs.ssh.core.sec.Permission.REQUEST_LIGHT_STATUS));
        assertTrue(permissionController.getTemplates().contains("Whaddaup"));

        //Check remove of none existing permission
        assertFalse(permissions.contains(de.unipassau.isl.evs.ssh.core.sec.Permission.SWITCH_LIGHT));
        permissionController.removePermissionFromTemplate("Whaddaup", de.unipassau.isl.evs.ssh.core.sec.Permission.SWITCH_LIGHT, "m1");

        //Check that permissions are removed from template
        permissions = new LinkedList<>();
        for (PermissionDTO permission : permissionController.getPermissionsOfTemplate("Whaddaup")) {
            permissions.add(permission.getPermission());
        }
        assertFalse(permissions.contains(de.unipassau.isl.evs.ssh.core.sec.Permission.REQUEST_LIGHT_STATUS));
        assertFalse(permissions.contains(de.unipassau.isl.evs.ssh.core.sec.Permission.SWITCH_LIGHT));

        //Remove permission w/ template refs
        permissionController.removePermission(de.unipassau.isl.evs.ssh.core.sec.Permission.REQUEST_LIGHT_STATUS, "m1");

        //Check that permission is gone
        permissions = new LinkedList<>();
        for (PermissionDTO permission : permissionController.getPermissionsOfTemplate("Whaddaup")) {
            permissions.add(permission.getPermission());
        }
        assertFalse(permissions.contains(de.unipassau.isl.evs.ssh.core.sec.Permission.REQUEST_LIGHT_STATUS));

        //Remove template w/ permission refs
        permissionController.removeTemplate("asdf");

        //Check that template is really gone
        assertEquals(permissionController.getPermissionsOfTemplate("asdf").size(), 0);

        //Check that permission is still there
        permissions = new LinkedList<>();
        for (PermissionDTO permission : permissionController.getPermissionsOfTemplate("Whaddaup")) {
            permissions.add(permission.getPermission());
        }
        assertTrue(permissions.contains(de.unipassau.isl.evs.ssh.core.sec.Permission.DELETE_ODROID));
    }

    public void testUserDevicesSlashGroupsAndPermissions() throws DatabaseControllerException {
        Context context = getInstrumentation().getTargetContext();
        //Clear database before running tests to assure clean test
        context.deleteDatabase(DatabaseConnector.DATABASE_NAME);
        SimpleContainer container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(context));
        container.register(DatabaseConnector.KEY, new DatabaseConnector());
        container.register(PermissionController.KEY, new PermissionController());
        container.register(UserManagementController.KEY, new UserManagementController());
        container.register(SlaveController.KEY, new SlaveController());
        PermissionController permissionController = container.require(PermissionController.KEY);
        UserManagementController userManagementController =
                container.require(UserManagementController.KEY);
        SlaveController slaveController = container.require(SlaveController.KEY);

        //Init stuff to test with. init already tested in testTemplatesAndPermissions
        permissionController.addTemplate("tmplll");
        //RenameTemplate
        permissionController.changeTemplateName("tmplll", "tmpl");
        permissionController.addTemplate("tmpl2");
        permissionController.addTemplate("tmpl3");
        permissionController.addPermission(de.unipassau.isl.evs.ssh.core.sec.Permission.ADD_GROUP, null);
        permissionController.addPermission(de.unipassau.isl.evs.ssh.core.sec.Permission.MODIFY_USER_PERMISSION, null);
        //PermissionDTO w/ module
        slaveController.addSlave(new Slave("abc", altDevice2, null));
        slaveController.addModule(new Module("m1", altDevice2, CoreConstants.ModuleType.DoorBuzzer,
                new USBAccessPoint(1)));
        permissionController.addPermission(de.unipassau.isl.evs.ssh.core.sec.Permission.SWITCH_LIGHT, "m1");

        //Rename to already existing
        try {
            permissionController.changeTemplateName("tmpl2", "tmpl");
            Assert.fail("PermissionDTO controller should have thrown AlreadyInUseException");
        } catch (AlreadyInUseException e) {
            assertFalse(false);
        }

        //Add same name
        try {
            permissionController.addTemplate("tmpl");
            Assert.fail("PermissionDTO controller should have thrown AlreadyInUseException");
        } catch (AlreadyInUseException e) {
            assertFalse(false);
        }

        //Add Group
        userManagementController.addGroup(new Group("grp1", "tmpl"));
        userManagementController.addGroup(new Group("grp2", "tmpl"));
        userManagementController.addGroup(new Group("grp3", "tmpl2"));

        //Use same Groupname twice
        try {
            userManagementController.addGroup(new Group("grp1", "tmpl"));
            Assert.fail("PermissionDTO controller should have thrown DatabaseControllerException");
        } catch (DatabaseControllerException e) {
            assertFalse(false);
        }

        //Test adding group with not existing template
        try {
            userManagementController.addGroup(new Group("dkjfkdfj", "aklsdjflasdjlf"));
            Assert.fail("PermissionDTO controller should have thrown DatabaseControllerException");
        } catch (DatabaseControllerException e) {
            assertFalse(false);
        }

        //Check if groups are added
        assertTrue(userManagementController.getGroup("grp1") != null);
        assertTrue(userManagementController.getGroup("grp2") != null);
        assertTrue(userManagementController.getGroup("grp3") != null);
        assertTrue(userManagementController.getGroups().size() == 6); // there are 3 default groups: Parents, Children, Guests

        //Check if groups have right template
        assertEquals(userManagementController.getGroup("grp1").getTemplateName(), "tmpl");
        assertEquals(userManagementController.getGroup("grp2").getTemplateName(), "tmpl");
        assertEquals(userManagementController.getGroup("grp3").getTemplateName(), "tmpl2");

        //Change template of group
        userManagementController.changeTemplateOfGroup("grp1", "tmpl3");
        assertEquals(userManagementController.getGroup("grp1").getTemplateName(), "tmpl3");

        //Change to none existing template
        try {
            userManagementController.changeTemplateOfGroup("grp1", "adfdfdsfdfs");
            Assert.fail("PermissionDTO controller should have thrown UnknownReferenceException");
        } catch (UnknownReferenceException e) {
            assertFalse(false);
        }

        //Test rename
        userManagementController.changeGroupName("grp1", "such wow");
        assertTrue(userManagementController.getGroups().size() == 6); // 3 extra default groups
        assertTrue(userManagementController.getGroup("such wow") != null);

        //Change to already existing name
        try {
            userManagementController.changeGroupName("grp3", "such wow");
            Assert.fail("PermissionDTO controller should have thrown AlreadyInUseException");
        } catch (AlreadyInUseException e) {
            assertFalse(false);
        }

        //Test removing groups
        userManagementController.removeGroup("grp2");
        assertTrue(userManagementController.getGroups().size() == 5); // 3 default groups
        List<String> groups = new LinkedList<>();
        for (Group group : userManagementController.getGroups()) {
            groups.add(group.getName());
        }
        assertTrue(groups.containsAll(Arrays.asList("such wow", "grp3")));

        //Remove template w/ group reference
        try {
            permissionController.removeTemplate("tmpl3");
            Assert.fail("PermissionDTO controller should have thrown IsReferencedException");
        } catch (IsReferencedException isReferencedException) {
            assertFalse(false);
        }

        //Test userdevice init
        userManagementController.addUserDevice(new UserDevice("u1", "such wow", device1));
        userManagementController.addUserDevice(new UserDevice("u2", "such wow", device2));
        userManagementController.addUserDevice(new UserDevice("u3", "grp3", device4));

        //Test adding userdevice with none existing group
        try {
            userManagementController.addUserDevice(
                    new UserDevice("askdfj", "--", altDevice2));
            Assert.fail("PermissionDTO controller should have thrown DatabaseControllerException");
        } catch (DatabaseControllerException e) {
            assertFalse(false);
        }

        //Remove group in use
        try {
            userManagementController.removeGroup("such wow");
            Assert.fail("PermissionDTO controller should have thrown IsReferencedException");
        } catch (IsReferencedException isReferencedException) {
            assertFalse(false);
        }

        //Check colliding
        try {
            userManagementController.addUserDevice(new UserDevice("u2", "such wow", device3));
            Assert.fail("PermissionDTO controller should have thrown DatabaseControllerException");
        } catch (DatabaseControllerException e) {
            assertFalse(false);
        }
        assertTrue(userManagementController.getUserDevices().size() == 3);
        assertTrue(userManagementController.getUserDevice(device1) != null);
        assertTrue(userManagementController.getUserDevice(device2) != null);
        assertTrue(userManagementController.getUserDevice(device3) == null);
        assertTrue(userManagementController.getUserDevice(device4) != null);

        //Check in group
        assertEquals(userManagementController.getUserDevice(device1).getInGroup(), "such wow");
        assertEquals(userManagementController.getUserDevice(device2).getInGroup(), "such wow");
        assertEquals(userManagementController.getUserDevice(device4).getInGroup(), "grp3");

        //Test permissions w/ userdevices
        permissionController.addUserPermission(device1, de.unipassau.isl.evs.ssh.core.sec.Permission.ADD_GROUP, null);
        permissionController.addUserPermission(device2, de.unipassau.isl.evs.ssh.core.sec.Permission.MODIFY_USER_PERMISSION, null);
        permissionController.addUserPermission(device4, de.unipassau.isl.evs.ssh.core.sec.Permission.MODIFY_USER_PERMISSION, null);
        permissionController.addUserPermission(device4, de.unipassau.isl.evs.ssh.core.sec.Permission.ADD_GROUP, null);
        permissionController.addUserPermission(device4, de.unipassau.isl.evs.ssh.core.sec.Permission.SWITCH_LIGHT, "m1");

        //Add to none device
        try {
            permissionController.addUserPermission(altDevice1, de.unipassau.isl.evs.ssh.core.sec.Permission.SWITCH_LIGHT, "m1");
            Assert.fail("PermissionDTO controller should have thrown UnknownReferenceException");
        } catch (UnknownReferenceException unknownReferenceException) {
            assertFalse(false);
        }

        //Remove permission
        permissionController.removeUserPermission(device4, de.unipassau.isl.evs.ssh.core.sec.Permission.MODIFY_USER_PERMISSION, null);
        //Remove permission user doesn't have
        permissionController.removeUserPermission(device1, de.unipassau.isl.evs.ssh.core.sec.Permission.MODIFY_USER_PERMISSION, null);
        //Remove permission from user that doesn't exist
        permissionController.removeUserPermission(altDevice1, de.unipassau.isl.evs.ssh.core.sec.Permission.MODIFY_USER_PERMISSION, null);

        //Check no changes to userdevices in database
        assertTrue(userManagementController.getUserDevices().size() == 3);
        assertTrue(userManagementController.getUserDevice(device1) != null);
        assertTrue(userManagementController.getUserDevice(device2) != null);
        assertTrue(userManagementController.getUserDevice(device3) == null);
        assertTrue(userManagementController.getUserDevice(device4) != null);

        //check removal of permissions
        assertTrue(permissionController.hasPermission(device2, de.unipassau.isl.evs.ssh.core.sec.Permission.MODIFY_USER_PERMISSION, null));
        assertFalse(permissionController.hasPermission(device1, de.unipassau.isl.evs.ssh.core.sec.Permission.MODIFY_USER_PERMISSION, null));
        assertFalse(permissionController.hasPermission(device4, de.unipassau.isl.evs.ssh.core.sec.Permission.MODIFY_USER_PERMISSION, null));
        assertTrue(permissionController.hasPermission(device4, de.unipassau.isl.evs.ssh.core.sec.Permission.ADD_GROUP, null));
        assertTrue(permissionController.hasPermission(device4, de.unipassau.isl.evs.ssh.core.sec.Permission.SWITCH_LIGHT, "m1"));

        //Test name change
        userManagementController.changeUserDeviceName(device1, "1u");
        userManagementController.changeUserDeviceName(device2, "11u");
        //Rename to same as other
        try {
            userManagementController.changeUserDeviceName(device4, "11u");
            Assert.fail("PermissionDTO controller should have thrown AlreadyInUseException");
        } catch (AlreadyInUseException e) {
            assertFalse(false);
        }
        assertTrue(userManagementController.getUserDevices().size() == 3);
        //Users still there
        assertTrue(userManagementController.getUserDevice(device1) != null);
        assertTrue(userManagementController.getUserDevice(device2) != null);
        assertTrue(userManagementController.getUserDevice(device3) == null);
        assertTrue(userManagementController.getUserDevice(device4) != null);
        //Users have new name
        assertEquals(userManagementController.getUserDevice(device1).getName(), "1u");
        assertEquals(userManagementController.getUserDevice(device2).getName(), "11u");
        assertEquals(userManagementController.getUserDevice(device4).getName(), "u3");


        //Test change group membership
        assertEquals(userManagementController.getUserDevice(device1).getInGroup(), "such wow");
        userManagementController.changeGroupMembership(device1, "grp3");
        assertEquals(userManagementController.getUserDevice(device1).getInGroup(), "grp3");

        //Change to none existing group
        try {
            userManagementController.changeGroupMembership(device1, "asdf");
            Assert.fail("PermissionDTO controller should have thrown UnknownReferenceException");
        } catch (UnknownReferenceException e) {
            assertFalse(false);
        }
        //Change from none existing user
        userManagementController.changeGroupMembership(altDevice1, "grp3");

        //Check user removal
        assertTrue(userManagementController.getUserDevice(device1) != null);
        userManagementController.removeUserDevice(device1);
        assertTrue(userManagementController.getUserDevice(device1) == null);
        //Remove none existing user
        userManagementController.removeUserDevice(altDevice1);
    }

    public void testSlaveController() throws DatabaseControllerException {
        Context context = getInstrumentation().getTargetContext();
        //Clear database before running tests to assure clean test
        context.deleteDatabase(DatabaseConnector.DATABASE_NAME);
        SimpleContainer container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(context));
        container.register(DatabaseConnector.KEY, new DatabaseConnector());
        container.register(SlaveController.KEY, new SlaveController());
        SlaveController slaveController = container.require(SlaveController.KEY);

        //Test slave init
        slaveController.addSlave(new Slave("s1", device1, null));
        slaveController.addSlave(new Slave("s2", device2, null));
        slaveController.addSlave(new Slave("s3", device3, null));
        List<String> slaves = new LinkedList<>();
        for (Slave slave : slaveController.getSlaves()) {
            slaves.add(slave.getName());
        }
        assertTrue(slaves.containsAll(Arrays.asList("s1", "s2", "s3")));

        //Add already existing slave
        try {
            slaveController.addSlave(new Slave("s1", altDevice1, null));
            Assert.fail("PermissionDTO controller should have thrown AlreadyInUseException");
        } catch (AlreadyInUseException e) {
            assertFalse(false);
        }
        try {
            slaveController.addSlave(new Slave("s55", device2, null));
            Assert.fail("PermissionDTO controller should have thrown AlreadyInUseException");
        } catch (AlreadyInUseException e) {
            assertFalse(false);
        }

        //Test modules init
        slaveController.addModule(new Module("m1", device1, CoreConstants.ModuleType.WeatherBoard,
                new USBAccessPoint(2)));
        slaveController.addModule(new Module("m2", device1, CoreConstants.ModuleType.Webcam,
                new USBAccessPoint(1)));
        assertNotNull(slaveController.getModule("m1"));
        assertNotNull(slaveController.getModule("m2"));
        assertTrue(slaveController.getModules().size() == 2);

        //Test to init modules at none existing slaves
        try {
            slaveController.addModule(new Module("m1", altDevice1, CoreConstants.ModuleType.DoorSensor,
                    new USBAccessPoint(2)));
            Assert.fail("PermissionDTO controller should have thrown DatabaseControllerException");
        } catch (DatabaseControllerException e) {
            assertFalse(false);
        }

        //Add module with already existing name
        try {
            slaveController.addModule(new Module("m2", device1, CoreConstants.ModuleType.DoorSensor,
                    new USBAccessPoint(1)));
            Assert.fail("PermissionDTO controller should have thrown DatabaseControllerException");
        } catch (DatabaseControllerException e) {
            assertFalse(false);
        }

        //Check if modules are on slave
        List<String> modules = new LinkedList<>();
        for (Module module : slaveController.getModulesOfSlave(device1)) {
            modules.add(module.getName());
        }
        assertTrue(modules.containsAll(Arrays.asList("m1", "m2")));
        assertTrue(modules.size() == 2);

        //Remove locked slave
        try {
            slaveController.removeSlave(device1);
            Assert.fail("PermissionDTO controller should have thrown IsReferencedException");
        } catch (IsReferencedException isReferencedException) {
            assertTrue(true);
        }
        //Remove slave
        slaveController.removeSlave(device2);
        slaves = new LinkedList<>();
        for (Slave slave : slaveController.getSlaves()) {
            slaves.add(slave.getName());
        }
        assertTrue(slaves.containsAll(Arrays.asList("s1", "s3")));

        //change slave names
        slaveController.changeSlaveName("s1", "abc");
        slaveController.changeSlaveName("s3", "abcd");
        slaves = new LinkedList<>();
        for (Slave slave : slaveController.getSlaves()) {
            slaves.add(slave.getName());
        }
        assertTrue(slaves.containsAll(Arrays.asList("abc", "abcd")));
        //Change to existing name
        try {
            slaveController.changeSlaveName("abcd", "abc");
            Assert.fail("PermissionDTO controller should have thrown AlreadyInUseException");
        } catch (AlreadyInUseException e) {
            assertFalse(false);
        }
        //Rename none existing slave
        slaveController.changeSlaveName("s2", "aa");

        //Change module name
        slaveController.changeModuleName("m1", "asdf");
        assertNotNull(slaveController.getModule("asdf"));
        assertNotNull(slaveController.getModule("m2"));
        assertTrue(slaveController.getModules().size() == 2);
        //Change to existing name
        try {
            slaveController.changeModuleName("m2", "asdf");
            Assert.fail("PermissionDTO controller should have thrown AlreadyInUseException");
        } catch (AlreadyInUseException e) {
            assertNotNull(slaveController.getModule("asdf"));
            assertNotNull(slaveController.getModule("m2"));
            assertTrue(slaveController.getModules().size() == 2);
        }

        //Remove module
        slaveController.removeModule("asdf");
        assertNull(slaveController.getModule("asdf"));
        assertNotNull(slaveController.getModule("m2"));
        assertTrue(slaveController.getModules().size() == 1);
    }

    public void testHolidayController() throws InterruptedException, DatabaseControllerException {
        Context context = getInstrumentation().getTargetContext();
        //Clear database before running tests to assure clean test
        context.deleteDatabase(DatabaseConnector.DATABASE_NAME);
        SimpleContainer container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(context));
        container.register(DatabaseConnector.KEY, new DatabaseConnector());
        container.register(HolidayController.KEY, new HolidayController());
        container.register(SlaveController.KEY, new SlaveController());
        HolidayController holidayController = container.require(HolidayController.KEY);
        SlaveController slaveController = container.require(SlaveController.KEY);

        slaveController.addSlave(new Slave("slave", device1, new byte[]{0, 0}));
        slaveController.addModule(new Module("module", device1, CoreConstants.ModuleType.Light, new USBAccessPoint(0)));

        long d1 = System.currentTimeMillis();
        Thread.sleep(1000);
        holidayController.addHolidayLogEntryNow("1", null);
        holidayController.addHolidayLogEntryNow("2", "module");
        Thread.sleep(1000);
        long d2 = System.currentTimeMillis();
        Thread.sleep(1000);
        holidayController.addHolidayLogEntryNow("3", null);
        holidayController.addHolidayLogEntryNow("4", "module");
        //Check illegal stuff add
        try {
            holidayController.addHolidayLogEntryNow(null, null);
            Assert.fail("PermissionDTO controller should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        Thread.sleep(1000);
        long d3 = System.currentTimeMillis();
        Thread.sleep(1000);

        List<HolidayAction> d1d2 = holidayController.getHolidayActions(d1, d2);
        List<String> d1d2Actions = new LinkedList<>();
        for (HolidayAction holidayAction : d1d2) {
            d1d2Actions.add(holidayAction.getActionName());
        }
        List<HolidayAction> d2d3 = holidayController.getHolidayActions(d2, d3);
        List<String> d2d3Actions = new LinkedList<>();
        for (HolidayAction holidayAction : d2d3) {
            d2d3Actions.add(holidayAction.getActionName());
        }
        List<HolidayAction> d1d3 = holidayController.getHolidayActions(d1, d3);
        List<String> d1d3Actions = new LinkedList<>();
        for (HolidayAction holidayAction : d1d3) {
            d1d3Actions.add(holidayAction.getActionName());
        }

        assertTrue(d1d2Actions.containsAll(Arrays.asList("1", "2")));
        assertTrue(d1d2Actions.size() == 2);
        assertTrue(d2d3Actions.containsAll(Arrays.asList("3", "4")));
        assertTrue(d2d3Actions.size() == 2);
        assertTrue(d1d3Actions.containsAll(Arrays.asList("1", "2", "3", "4")));
        assertTrue(d1d3Actions.size() == 4);
    }
}