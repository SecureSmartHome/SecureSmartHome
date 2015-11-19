package de.unipassau.isl.evs.ssh.master.database;

import android.content.Context;
import android.test.InstrumentationTestCase;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.dto.Group;
import de.unipassau.isl.evs.ssh.master.database.dto.Module;
import de.unipassau.isl.evs.ssh.master.database.dto.ModuleAccessPoint.USBAccessPoint;
import de.unipassau.isl.evs.ssh.master.database.dto.Slave;
import de.unipassau.isl.evs.ssh.master.database.dto.UserDevice;

public class ControllerTest extends InstrumentationTestCase {

    public void testTemplatesAndPermissions() throws IsReferencedException, IllegalReferenceException, AlreadyInUseException {
        Context context = getInstrumentation().getTargetContext();
        //Clear database before running tests to assure clean test
        context.deleteDatabase(DatabaseConnector.DBOpenHelper.DATABASE_NAME);
        SimpleContainer container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(context));
        container.register(DatabaseConnector.KEY, new DatabaseConnector());
        container.register(PermissionController.KEY, new PermissionController());
        PermissionController permissionController = container.require(PermissionController.KEY);

        //Test Templates w/ permissions
        //Add Templates
        permissionController.addTemplate("Whaddaup");
        permissionController.addTemplate("asdf");

        //Add Permissions
        permissionController.addPermission("test");
        permissionController.addPermission("test2");
        permissionController.addPermission("test3");
        permissionController.addPermission("test4");

        //Add same name
        try {
            permissionController.addPermission("test");
        } catch (AlreadyInUseException e) {
            assertFalse(false);
        }

        //Check that permissions are inserted
        assertTrue(permissionController.getPermissions().containsAll(Arrays.asList("test", "test2", "test3", "test4")));

        //Remove Permission
        permissionController.removePermission("test4");

        //Check that permission is removed
        assertFalse(permissionController.getPermissions().contains("test4"));

        //Remove element that is already removed
        permissionController.removePermission("test4");

        //Check that it's still not in database and not getting added again magically ~~~
        assertFalse(permissionController.getPermissions().contains("test4"));

        //Add permissions to templates.
        permissionController.addPermissionToTemplate("Whaddaup", "test");
        permissionController.addPermissionToTemplate("Whaddaup", "test2");
        permissionController.addPermissionToTemplate("Whaddaup", "test3");
        permissionController.addPermissionToTemplate("asdf", "test");

        //Add none existing permissions or template
        try {
            permissionController.addPermissionToTemplate("asdf", "zzz");
        } catch (IllegalReferenceException illegalReferenceException) {
            assertFalse(false);
        }
        try {
            permissionController.addPermissionToTemplate("44", "test");
        } catch (IllegalReferenceException illegalReferenceException) {
            assertFalse(false);
        }

        //Check that permissions are in template
        List<String> permissions;
        permissions = permissionController.getPermissionsOfTemplate("Whaddaup");
        assertTrue(permissions.containsAll(Arrays.asList("test", "test2", "test3")));

        //Check that no additional stuff is there
        assertTrue(permissions.size() == 3);

        //Check that permissions are in template
        permissions = permissionController.getPermissionsOfTemplate("asdf");
        assertTrue(permissions.contains("test"));

        //Check that no additional stuff is there
        assertTrue(permissions.size() == 1);

        //Remove permissions from template
        permissionController.removePermissionFromTemplate("Whaddaup", "test3");

        //Check that template and permission removed from template still exist
        assertTrue(permissionController.getPermissions().contains("test3"));
        assertTrue(permissionController.getTemplates().contains("Whaddaup"));

        //Check remove of none existing permission
        assertFalse(permissionController.getPermissions().contains("test4"));
        permissionController.removePermissionFromTemplate("Whaddaup", "test4");

        //Check that permissions are removed from template
        permissions = permissionController.getPermissionsOfTemplate("Whaddaup");
        assertFalse(permissions.contains("test3"));
        assertFalse(permissions.contains("test4"));

        //Remove permission w/ template refs
        permissionController.removePermission("test3");

        //Check that permission is gone
        assertFalse(permissionController.getPermissions().contains("test3"));

        //Remove template w/ permission refs
        permissionController.removeTemplate("asdf");

        //Check that template is really gone
        permissions = permissionController.getPermissionsOfTemplate("asdf");
        assertEquals(permissions.size(), 0);

        //Check that permission is still there
        assertTrue(permissionController.getPermissions().contains("test"));
    }

    public void testUserDevicesSlashGroupsAndPermissions() throws DatabaseControllerException {
        Context context = getInstrumentation().getTargetContext();
        //Clear database before running tests to assure clean test
        context.deleteDatabase(DatabaseConnector.DBOpenHelper.DATABASE_NAME);
        SimpleContainer container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(context));
        container.register(DatabaseConnector.KEY, new DatabaseConnector());
        container.register(PermissionController.KEY, new PermissionController());
        container.register(UserManagementController.KEY, new UserManagementController());
        PermissionController permissionController = container.require(PermissionController.KEY);
        UserManagementController userManagementController =
                container.require(UserManagementController.KEY);

        //Init stuff to test with. init already tested in testTemplatesAndPermissions
        permissionController.addTemplate("tmplll");
        //RenameTemplate
        permissionController.changeTemplateName("tmplll", "tmpl");
        permissionController.addTemplate("tmpl2");
        permissionController.addTemplate("tmpl3");
        permissionController.addPermission("perm1");
        permissionController.addPermission("perm2");
        permissionController.addPermission("perm3");

        //Rename to already existing
        try {
            permissionController.changeTemplateName("tmpl2", "tmpl");
        } catch (AlreadyInUseException e) {
            assertFalse(false);
        }

        //Add same name
        try {
            permissionController.addTemplate("tmpl");
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
        } catch (DatabaseControllerException e) {
            assertFalse(false);
        }

        //Test adding group with not existing template
        try {
            userManagementController.addGroup(new Group("dkjfkdfj", "aklsdjflasdjlf"));
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
        } catch (IllegalReferenceException e) {
            assertFalse(false);
        }

        //Test rename
        userManagementController.changeGroupName("grp1", "such wow");
        assertTrue(userManagementController.getGroups().size() == 3);
        assertTrue(userManagementController.getGroup("such wow") != null);

        //Change to already existing name
        try {
            userManagementController.changeGroupName("grp3", "such wow");
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
        } catch (DatabaseControllerException e) {
            assertFalse(false);
        }

        //Remove group in use
        try {
            userManagementController.removeGroup("such wow");
        } catch (IsReferencedException isReferencedException) {
            assertFalse(false);
        }

        //Check colliding
        try {
            userManagementController.addUserDevice(new UserDevice("u2", "such wow", new DeviceID("3")));
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
        permissionController.addUserPermission(new DeviceID("1"), "perm1");
        permissionController.addUserPermission(new DeviceID("2"), "perm2");
        permissionController.addUserPermission(new DeviceID("4"), "perm2");
        permissionController.addUserPermission(new DeviceID("4"), "perm1");
        permissionController.addUserPermission(new DeviceID("4"), "perm3");

        //Add none existing permissions or devices
        try {
            permissionController.addUserPermission(new DeviceID("4"), "zzz");
        } catch (IllegalReferenceException illegalReferenceException) {
            assertFalse(false);
        }
        try {
            permissionController.addUserPermission(new DeviceID("44"), "perm3");
        } catch (IllegalReferenceException illegalReferenceException) {
            assertFalse(false);
        }

        //Remove permission
        permissionController.removeUserPermission(new DeviceID("4"), "perm2");
        //Remove permission user doesn't have
        permissionController.removeUserPermission(new DeviceID("1"), "perm2");
        //Remove permission user that doesn't exist
        permissionController.removeUserPermission(new DeviceID("1"), "asdfas");
        //Remove permission from user that doesn't exist
        permissionController.removeUserPermission(new DeviceID("1000"), "perm2");

        //Check no changes to userdevices in database
        assertTrue(userManagementController.getUserDevices().size() == 3);
        assertTrue(userManagementController.getUserDevice(new DeviceID("1")) != null);
        assertTrue(userManagementController.getUserDevice(new DeviceID("2")) != null);
        assertTrue(userManagementController.getUserDevice(new DeviceID("3")) == null);
        assertTrue(userManagementController.getUserDevice(new DeviceID("4")) != null);

        //check removal of permissions
        assertTrue(permissionController.hasPermission(new DeviceID("2"), "perm2"));
        assertFalse(permissionController.hasPermission(new DeviceID("2"), "asdf"));
        assertFalse(permissionController.hasPermission(new DeviceID("1"), "perm2"));
        assertFalse(permissionController.hasPermission(new DeviceID("4"), "perm2"));
        assertTrue(permissionController.hasPermission(new DeviceID("4"), "perm1"));
        assertTrue(permissionController.hasPermission(new DeviceID("4"), "perm3"));

        //Test name change
        userManagementController.changeUserDeviceName("u1", "1u");
        userManagementController.changeUserDeviceName("u2", "11u");
        //Rename to same as other
        try {
            userManagementController.changeUserDeviceName("u3", "11u");
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
        } catch (IllegalReferenceException e) {
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
        context.deleteDatabase(DatabaseConnector.DBOpenHelper.DATABASE_NAME);
        SimpleContainer container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(context));
        container.register(DatabaseConnector.KEY, new DatabaseConnector());
        container.register(SlaveController.KEY, new SlaveController());
        SlaveController slaveController = container.require(SlaveController.KEY);

        //Test slave init
        slaveController.addSlave(new Slave("s1", new DeviceID("1")));
        slaveController.addSlave(new Slave("s2", new DeviceID("2")));
        slaveController.addSlave(new Slave("s3", new DeviceID("3")));
        List<String> slaves = new LinkedList<>();
        for (Slave slave : slaveController.getSlaves()) {
            slaves.add(slave.getName());
        }
        assertTrue(slaves.containsAll(Arrays.asList("s1", "s2", "s3")));

        //Add already existing slave
        try {
            slaveController.addSlave(new Slave("s1", new DeviceID("55")));
        } catch (AlreadyInUseException e ) {
            assertFalse(false);
        }
        try {
            slaveController.addSlave(new Slave("s55", new DeviceID("2")));
        } catch (AlreadyInUseException e ) {
            assertFalse(false);
        }

        //Test modules init
        slaveController.addModule(new Module("m1", new DeviceID("1"), new USBAccessPoint(2)));
        slaveController.addModule(new Module("m2", new DeviceID("1"), new USBAccessPoint(1)));
        assertNotNull(slaveController.getModule("m1"));
        assertNotNull(slaveController.getModule("m2"));
        assertTrue(slaveController.getModules().size() == 2);

        //Test to init modules at none existing slaves
        try {
            slaveController.addModule(new Module("m1", new DeviceID("99"), new USBAccessPoint(2)));
        } catch (DatabaseControllerException e) {
            assertFalse(false);
        }

        //Add module with already existing name
        try {
            slaveController.addModule(new Module("m2", new DeviceID("1"), new USBAccessPoint(1)));
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
        context.deleteDatabase(DatabaseConnector.DBOpenHelper.DATABASE_NAME);
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