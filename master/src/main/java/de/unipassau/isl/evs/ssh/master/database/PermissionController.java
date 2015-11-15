package de.unipassau.isl.evs.ssh.master.database;

import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.util.DeviceID;
import de.unipassau.isl.evs.ssh.master.database.dto.Permission;
import de.unipassau.isl.evs.ssh.master.database.dto.UserPermission;

/**
 * Offers high level methods to interact with the tables associated with permissions in the database.
 */
public class PermissionController extends AbstractComponent {
    public static final Key<PermissionController> KEY = new Key<>(PermissionController.class);

    /**
     * Lists all permissions of a given template.
     *
     * @param templateName Name of the template.
     * @return List of the permissions in the template
     */
    public List<Permission> getTemplate(String templateName) {
        // TODO - implement PermissionController.getTemplate
        throw new UnsupportedOperationException();
    }

    /**
     * Returns whether a given user has a given permission.
     *
     * @param userDeviceID   DeviceID associated with the user.
     * @param permissionName Name of the permission.
     * @return true if has permissions otherwise false.
     */
    public boolean hasPermission(DeviceID userDeviceID, String permissionName) {
        // TODO - implement PermissionController.hasPermission
        throw new UnsupportedOperationException();
    }

    /**
     * Removes a template from the database.
     *
     * @param templateName Name of the template.
     */
    public void removeTemplate(String templateName) {
        // TODO - implement PermissionController.removeTemplate
        throw new UnsupportedOperationException();
    }

    /**
     * Set single permission in a template.
     *
     * @param templateName Name of the template to set permission in.
     * @param permission   Permission to update.
     */
    public void setPermissionInTemplate(String templateName, Permission permission) {
        // TODO - implement PermissionController.setPermissionInTemplate
        throw new UnsupportedOperationException();
    }

    /**
     * Updates a given permission.
     *
     * @param userPermission Permissions to set.
     */
    public void setPermission(UserPermission userPermission) {
        // TODO - implement PermissionController.setPermission
        throw new UnsupportedOperationException();
    }

}