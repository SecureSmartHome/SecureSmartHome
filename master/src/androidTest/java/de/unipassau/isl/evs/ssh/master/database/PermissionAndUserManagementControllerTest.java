package de.unipassau.isl.evs.ssh.master.database;

import android.test.InstrumentationTestCase;

import java.util.List;

import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;
import de.unipassau.isl.evs.ssh.master.database.dto.Permission;

public class PermissionAndUserManagementControllerTest extends InstrumentationTestCase {

    public void testAll() {
        SimpleContainer container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(getInstrumentation().getTargetContext()));
        container.register(DatabaseConnector.KEY, new DatabaseConnector());
        container.register(PermissionController.KEY, new PermissionController());
        container.register(UserManagementController.KEY, new UserManagementController());
        PermissionController permissionController = container.require(PermissionController.KEY);
        UserManagementController userManagementController =
                container.require(UserManagementController.KEY);

        //Template test
        permissionController.addTemplate("Whaddaup");
        permissionController.addTemplate("asdf");
        permissionController.addPermission("test");
        permissionController.addPermission("test2");
        permissionController.addPermission("test3");
        permissionController.addPermission("test4");
        permissionController.removePermission("test4");
        permissionController.setPermissionInTemplate("Whaddaup", new Permission("test", true));
        permissionController.setPermissionInTemplate("Whaddaup", new Permission("test2", true));
        permissionController.setPermissionInTemplate("Whaddaup", new Permission("test3", true));
        permissionController.setPermissionInTemplate("asdf", new Permission("test", true));
        permissionController.setPermissionInTemplate("Whaddaup", new Permission("test3", false));
        permissionController.setPermissionInTemplate("Whaddaup", new Permission("test4", false));
        List<Permission> permissions = permissionController.getTemplate("Whaddaup");
        assertEquals(permissions.get(0).getPermissionName(), "test");
        assertEquals(permissions.get(1).getPermissionName(), "test2");
        assertEquals(permissions.size(), 2);
        permissionController.removeTemplate("Whaddaup");
        permissions = permissionController.getTemplate("Whaddaup");
        assertEquals(permissions.size(), 0);
    }
}