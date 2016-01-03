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
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.USBAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * Test class for all controller functionality (database interface)
 *
 * @author Leon Sell
 */
public class ControllerTest extends InstrumentationTestCase {

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
        slaveController.addSlave(new Slave("s1", new DeviceID("1"), null));
        slaveController.addModule(new Module("m1", new DeviceID("1"), CoreConstants.ModuleType.LIGHT,
                new USBAccessPoint(1)));

        //Add Permissions
        permissionController.addPermission(new Permission("test"));
        permissionController.addPermission(new Permission("test2", null));
        permissionController.addPermission(new Permission("test3", "m1"));
        permissionController.addPermission(new Permission("test4", "m1"));

        //Add same name. Will not fail when module is null for both and name is same.
        try {
            permissionController.addPermission(new Permission("test3", "m1"));
            Assert.fail("Permission controller should have thrown AlreadyInUseException");
        } catch (AlreadyInUseException e) {
            assertFalse(false);
        }

        //Check that permissions are inserted
        List<String> permissions = new LinkedList<>();
        for (Permission permission : permissionController.getPermissions()) {
            permissions.add(permission.getName()); // as they are unique in this testcase and i don't want to implement
            // hashcode just for this test, just the name works fine.
        }
        assertTrue(permissions.containsAll(Arrays.asList("test", "test2", "test3", "test4")));

        //Remove Permission
        permissionController.removePermission(new Permission("test4", "m1"));

        //Check that permission is removed
        permissions = new LinkedList<>();
        for (Permission permission : permissionController.getPermissions()) {
            permissions.add(permission.getName());
        }
        assertFalse(permissions.contains("test4"));

        //Remove element that is already removed
        permissionController.removePermission(new Permission("test4", "m1"));

        //Check that it's still not in database and not getting added again magically ~~~
        permissions = new LinkedList<>();
        for (Permission permission : permissionController.getPermissions()) {
            permissions.add(permission.getName());
        }
        assertFalse(permissions.contains("test4"));

        //Add permissions to templates.
        permissionController.addPermissionToTemplate("Whaddaup", new Permission("test"));
        permissionController.addPermissionToTemplate("Whaddaup", new Permission("test2"));
        permissionController.addPermissionToTemplate("Whaddaup", new Permission("test3", "m1"));
        permissionController.addPermissionToTemplate("asdf", new Permission("test"));

        //Add none existing permissions or template
        try {
            permissionController.addPermissionToTemplate("asdf", new Permission("zzz"));
            Assert.fail("Permission controller should have thrown UnknownReferenceException");
        } catch (UnknownReferenceException unknownReferenceException) {
            assertFalse(false);
        }
        try {
            permissionController.addPermissionToTemplate("44", new Permission("test"));
            Assert.fail("Permission controller should have thrown UnknownReferenceException");
        } catch (UnknownReferenceException unknownReferenceException) {
            assertFalse(false);
        }

        //Check that permissions are in template
        permissions = new LinkedList<>();
        for (Permission permission : permissionController.getPermissionsOfTemplate("Whaddaup")) {
            permissions.add(permission.getName());
        }
        assertTrue(permissions.containsAll(Arrays.asList("test", "test2", "test3")));

        //Check that permissions are in template
        permissions = new LinkedList<>();
        for (Permission permission : permissionController.getPermissionsOfTemplate("asdf")) {
            permissions.add(permission.getName());
        }
        assertTrue(permissions.contains("test"));

        //Remove permissions from template
        permissionController.removePermissionFromTemplate("Whaddaup", new Permission("test3", "m1"));

        //Check that template and permission removed from template still exist
        permissions = new LinkedList<>();
        for (Permission permission : permissionController.getPermissions()) {
            permissions.add(permission.getName());
        }

        assertTrue(permissions.contains("test3"));
        assertTrue(permissionController.getTemplates().contains("Whaddaup"));

        //Check remove of none existing permission
        assertFalse(permissions.contains("test4"));
        permissionController.removePermissionFromTemplate("Whaddaup", new Permission("test4"));

        //Check that permissions are removed from template
        permissions = new LinkedList<>();
        for (Permission permission : permissionController.getPermissionsOfTemplate("Whaddaup")) {
            permissions.add(permission.getName());
        }
        assertFalse(permissions.contains("test3"));
        assertFalse(permissions.contains("test4"));

        //Remove permission w/ template refs
        permissionController.removePermission(new Permission("test3", "m1"));

        //Check that permission is gone
        permissions = new LinkedList<>();
        for (Permission permission : permissionController.getPermissionsOfTemplate("Whaddaup")) {
            permissions.add(permission.getName());
        }
        assertFalse(permissions.contains("test3"));

        //Remove template w/ permission refs
        permissionController.removeTemplate("asdf");

        //Check that template is really gone
        assertEquals(permissionController.getPermissionsOfTemplate("asdf").size(), 0);

        //Check that permission is still there
        permissions = new LinkedList<>();
        for (Permission permission : permissionController.getPermissionsOfTemplate("Whaddaup")) {
            permissions.add(permission.getName());
        }
        assertTrue(permissions.contains("test"));
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
        permissionController.addPermission(new Permission("perm1"));
        permissionController.addPermission(new Permission("perm2"));
        //Permission w/ module
        slaveController.addSlave(new Slave("abc", new DeviceID("111"), null));
        slaveController.addModule(new Module("m1", new DeviceID("111"), CoreConstants.ModuleType.DOOR_BUZZER,
                new USBAccessPoint(1)));
        permissionController.addPermission(new Permission("perm3", "m1"));

        //Rename to already existing
        try {
            permissionController.changeTemplateName("tmpl2", "tmpl");
            Assert.fail("Permission controller should have thrown AlreadyInUseException");
        } catch (AlreadyInUseException e) {
            assertFalse(false);
        }

        //Add same name
        try {
            permissionController.addTemplate("tmpl");
            Assert.fail("Permission controller should have thrown AlreadyInUseException");
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
            Assert.fail("Permission controller should have thrown DatabaseControllerException");
        } catch (DatabaseControllerException e) {
            assertFalse(false);
        }

        //Test adding group with not existing template
        try {
            userManagementController.addGroup(new Group("dkjfkdfj", "aklsdjflasdjlf"));
            Assert.fail("Permission controller should have thrown DatabaseControllerException");
        } catch (DatabaseControllerException e) {
            assertFalse(false);
        }

        //Check if groups are added
        assertTrue(userManagementController.getGroup("grp1") != null);
        assertTrue(userManagementController.getGroup("grp2") != null);
        assertTrue(userManagementController.getGroup("grp3") != null);
        assertTrue(userManagementController.getGroups().size() == 3);

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
            Assert.fail("Permission controller should have thrown UnknownReferenceException");
        } catch (UnknownReferenceException e) {
            assertFalse(false);
        }

        //Test rename
        userManagementController.changeGroupName("grp1", "such wow");
        assertTrue(userManagementController.getGroups().size() == 3);
        assertTrue(userManagementController.getGroup("such wow") != null);

        //Change to already existing name
        try {
            userManagementController.changeGroupName("grp3", "such wow");
            Assert.fail("Permission controller should have thrown AlreadyInUseException");
        } catch (AlreadyInUseException e) {
            assertFalse(false);
        }

        //Test removing groups
        userManagementController.removeGroup("grp2");
        assertTrue(userManagementController.getGroups().size() == 2);
        List<String> groups = new LinkedList<>();
        for (Group group : userManagementController.getGroups()) {
            groups.add(group.getName());
        }
        assertTrue(groups.containsAll(Arrays.asList("such wow", "grp3")));

        //Remove template w/ group reference
        try {
            permissionController.removeTemplate("tmpl3");
            Assert.fail("Permission controller should have thrown IsReferencedException");
        } catch (IsReferencedException isReferencedException) {
            assertFalse(false);
        }

        //Test userdevice init
        userManagementController.addUserDevice(new UserDevice("u1", "such wow", new DeviceID("1")));
        userManagementController.addUserDevice(new UserDevice("u2", "such wow", new DeviceID("2")));
        userManagementController.addUserDevice(new UserDevice("u3", "grp3", new DeviceID("4")));

        //Test adding userdevice with none existing group
        try {
            userManagementController.addUserDevice(
                    new UserDevice("askdfj", "--", new DeviceID("20")));
            Assert.fail("Permission controller should have thrown DatabaseControllerException");
        } catch (DatabaseControllerException e) {
            assertFalse(false);
        }

        //Remove group in use
        try {
            userManagementController.removeGroup("such wow");
            Assert.fail("Permission controller should have thrown IsReferencedException");
        } catch (IsReferencedException isReferencedException) {
            assertFalse(false);
        }

        //Check colliding
        try {
            userManagementController.addUserDevice(new UserDevice("u2", "such wow", new DeviceID("3")));
            Assert.fail("Permission controller should have thrown DatabaseControllerException");
        } catch (DatabaseControllerException e) {
            assertFalse(false);
        }
        assertTrue(userManagementController.getUserDevices().size() == 3);
        assertTrue(userManagementController.getUserDevice(new DeviceID("1")) != null);
        assertTrue(userManagementController.getUserDevice(new DeviceID("2")) != null);
        assertTrue(userManagementController.getUserDevice(new DeviceID("3")) == null);
        assertTrue(userManagementController.getUserDevice(new DeviceID("4")) != null);

        //Check in group
        assertEquals(userManagementController.getUserDevice(new DeviceID("1")).getInGroup(), "such wow");
        assertEquals(userManagementController.getUserDevice(new DeviceID("2")).getInGroup(), "such wow");
        assertEquals(userManagementController.getUserDevice(new DeviceID("4")).getInGroup(), "grp3");

        //Test permissions w/ userdevices
        permissionController.addUserPermission(new DeviceID("1"), new Permission("perm1"));
        permissionController.addUserPermission(new DeviceID("2"), new Permission("perm2"));
        permissionController.addUserPermission(new DeviceID("4"), new Permission("perm2"));
        permissionController.addUserPermission(new DeviceID("4"), new Permission("perm1"));
        permissionController.addUserPermission(new DeviceID("4"), new Permission("perm3", "m1"));

        //Add none existing permissions or devices
        try {
            permissionController.addUserPermission(new DeviceID("4"), new Permission("zzz"));
            Assert.fail("Permission controller should have thrown UnknownReferenceException");
        } catch (UnknownReferenceException unknownReferenceException) {
            assertFalse(false);
        }
        try {
            permissionController.addUserPermission(new DeviceID("44"), new Permission("perm3", "m1"));
            Assert.fail("Permission controller should have thrown UnknownReferenceException");
        } catch (UnknownReferenceException unknownReferenceException) {
            assertFalse(false);
        }

        //Remove permission
        permissionController.removeUserPermission(new DeviceID("4"), new Permission("perm2"));
        //Remove permission user doesn't have
        permissionController.removeUserPermission(new DeviceID("1"), new Permission("perm2"));
        //Remove permission user that doesn't exist
        permissionController.removeUserPermission(new DeviceID("1"), new Permission("asdfas"));
        //Remove permission from user that doesn't exist
        permissionController.removeUserPermission(new DeviceID("1000"), new Permission("perm2"));

        //Check no changes to userdevices in database
        assertTrue(userManagementController.getUserDevices().size() == 3);
        assertTrue(userManagementController.getUserDevice(new DeviceID("1")) != null);
        assertTrue(userManagementController.getUserDevice(new DeviceID("2")) != null);
        assertTrue(userManagementController.getUserDevice(new DeviceID("3")) == null);
        assertTrue(userManagementController.getUserDevice(new DeviceID("4")) != null);

        //check removal of permissions
        assertTrue(permissionController.hasPermission(new DeviceID("2"), new Permission("perm2")));
        assertFalse(permissionController.hasPermission(new DeviceID("2"), new Permission("asdf")));
        assertFalse(permissionController.hasPermission(new DeviceID("1"), new Permission("perm2")));
        assertFalse(permissionController.hasPermission(new DeviceID("4"), new Permission("perm2")));
        assertTrue(permissionController.hasPermission(new DeviceID("4"), new Permission("perm1")));
        assertTrue(permissionController.hasPermission(new DeviceID("4"), new Permission("perm3", "m1")));

        //Test name change
        userManagementController.changeUserDeviceName("u1", "1u");
        userManagementController.changeUserDeviceName("u2", "11u");
        //Rename to same as other
        try {
            userManagementController.changeUserDeviceName("u3", "11u");
            Assert.fail("Permission controller should have thrown AlreadyInUseException");
        } catch (AlreadyInUseException e) {
            assertFalse(false);
        }
        assertTrue(userManagementController.getUserDevices().size() == 3);
        //Users still there
        assertTrue(userManagementController.getUserDevice(new DeviceID("1")) != null);
        assertTrue(userManagementController.getUserDevice(new DeviceID("2")) != null);
        assertTrue(userManagementController.getUserDevice(new DeviceID("3")) == null);
        assertTrue(userManagementController.getUserDevice(new DeviceID("4")) != null);
        //Users have new name
        assertEquals(userManagementController.getUserDevice(new DeviceID("1")).getName(), "1u");
        assertEquals(userManagementController.getUserDevice(new DeviceID("2")).getName(), "11u");
        assertEquals(userManagementController.getUserDevice(new DeviceID("4")).getName(), "u3");


        //Test change group membership
        assertEquals(userManagementController.getUserDevice(new DeviceID("1")).getInGroup(), "such wow");
        userManagementController.changeGroupMembership(new DeviceID("1"), "grp3");
        assertEquals(userManagementController.getUserDevice(new DeviceID("1")).getInGroup(), "grp3");

        //Change to none existing group
        try {
            userManagementController.changeGroupMembership(new DeviceID("1"), "asdf");
            Assert.fail("Permission controller should have thrown UnknownReferenceException");
        } catch (UnknownReferenceException e) {
            assertFalse(false);
        }
        //Change from none existing user
        userManagementController.changeGroupMembership(new DeviceID("asdfjasldf"), "grp3");

        //Check user removal
        assertTrue(userManagementController.getUserDevice(new DeviceID("1")) != null);
        userManagementController.removeUserDevice(new DeviceID("1"));
        assertTrue(userManagementController.getUserDevice(new DeviceID("1")) == null);
        //Remove none existing user
        userManagementController.removeUserDevice(new DeviceID("200"));
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
        slaveController.addSlave(new Slave("s1", new DeviceID("1"), null));
        slaveController.addSlave(new Slave("s2", new DeviceID("2"), null));
        slaveController.addSlave(new Slave("s3", new DeviceID("3"), null));
        List<String> slaves = new LinkedList<>();
        for (Slave slave : slaveController.getSlaves()) {
            slaves.add(slave.getName());
        }
        assertTrue(slaves.containsAll(Arrays.asList("s1", "s2", "s3")));

        //Add already existing slave
        try {
            slaveController.addSlave(new Slave("s1", new DeviceID("55"), null));
            Assert.fail("Permission controller should have thrown AlreadyInUseException");
        } catch (AlreadyInUseException e) {
            assertFalse(false);
        }
        try {
            slaveController.addSlave(new Slave("s55", new DeviceID("2"), null));
            Assert.fail("Permission controller should have thrown AlreadyInUseException");
        } catch (AlreadyInUseException e) {
            assertFalse(false);
        }

        //Test modules init
        slaveController.addModule(new Module("m1", new DeviceID("1"), CoreConstants.ModuleType.WEATHER_BOARD,
                new USBAccessPoint(2)));
        slaveController.addModule(new Module("m2", new DeviceID("1"), CoreConstants.ModuleType.WEBCAM,
                new USBAccessPoint(1)));
        assertNotNull(slaveController.getModule("m1"));
        assertNotNull(slaveController.getModule("m2"));
        assertTrue(slaveController.getModules().size() == 2);

        //Test to init modules at none existing slaves
        try {
            slaveController.addModule(new Module("m1", new DeviceID("99"), CoreConstants.ModuleType.DOOR_SENSOR,
                    new USBAccessPoint(2)));
            Assert.fail("Permission controller should have thrown DatabaseControllerException");
        } catch (DatabaseControllerException e) {
            assertFalse(false);
        }

        //Add module with already existing name
        try {
            slaveController.addModule(new Module("m2", new DeviceID("1"), CoreConstants.ModuleType.DOOR_SENSOR,
                    new USBAccessPoint(1)));
            Assert.fail("Permission controller should have thrown DatabaseControllerException");
        } catch (DatabaseControllerException e) {
            assertFalse(false);
        }

        //Check if modules are on slave
        List<String> modules = new LinkedList<>();
        for (Module module : slaveController.getModulesOfSlave(new DeviceID("1"))) {
            modules.add(module.getName());
        }
        assertTrue(modules.containsAll(Arrays.asList("m1", "m2")));
        assertTrue(modules.size() == 2);

        //Remove locked slave
        try {
            slaveController.removeSlave(new DeviceID("1"));
            Assert.fail("Permission controller should have thrown IsReferencedException");
        } catch (IsReferencedException isReferencedException) {
            assertTrue(true);
        }
        //Remove slave
        slaveController.removeSlave(new DeviceID("2"));
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
            Assert.fail("Permission controller should have thrown AlreadyInUseException");
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
            Assert.fail("Permission controller should have thrown AlreadyInUseException");
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

    public void testHolidayController() throws InterruptedException {
        Context context = getInstrumentation().getTargetContext();
        //Clear database before running tests to assure clean test
        context.deleteDatabase(DatabaseConnector.DATABASE_NAME);
        SimpleContainer container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(context));
        container.register(DatabaseConnector.KEY, new DatabaseConnector());
        container.register(HolidayController.KEY, new HolidayController());
        HolidayController holidayController = container.require(HolidayController.KEY);

        //Add suff
        Date d1 = new Date(System.currentTimeMillis());
        Thread.sleep(1000);
        holidayController.addHolidayLogEntry("1");
        holidayController.addHolidayLogEntry("2");
        Thread.sleep(1000);
        Date d2 = new Date(System.currentTimeMillis());
        Thread.sleep(1000);
        holidayController.addHolidayLogEntry("3");
        holidayController.addHolidayLogEntry("4");
        //Check illegal stuff add
        try {
            holidayController.addHolidayLogEntry(null);
            Assert.fail("Permission controller should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        Thread.sleep(1000);
        Date d3 = new Date(System.currentTimeMillis());
        Thread.sleep(1000);

        assertTrue(holidayController.getLogEntriesRange(d1, d2).containsAll(Arrays.asList("1", "2")));
        assertTrue(holidayController.getLogEntriesRange(d1, d2).size() == 2);
        assertTrue(holidayController.getLogEntriesRange(d2, d3).containsAll(Arrays.asList("3", "4")));
        assertTrue(holidayController.getLogEntriesRange(d2, d3).size() == 2);
        assertTrue(holidayController.getLogEntriesRange(d1, d3).containsAll(Arrays.asList("1", "2", "3", "4")));
        assertTrue(holidayController.getLogEntriesRange(d1, d3).size() == 4);
    }
}