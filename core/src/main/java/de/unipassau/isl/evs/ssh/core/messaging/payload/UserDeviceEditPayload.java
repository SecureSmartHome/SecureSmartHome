package de.unipassau.isl.evs.ssh.core.messaging.payload;

import java.util.HashMap;
import java.util.Map;

import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;

public class UserDeviceEditPayload implements MessagePayload {

    private Permission permission;
    private UserDevice userDevice;
    private UserDevice oldUserDevice;
    private UserDevice newUserDevice;
    private Action action;

    private UserDeviceEditPayload() {

    }

    public enum Action {
        ADD_USERDEVICE, REMOVE_USERDEVICE,
        EDIT_USERDEVICE,
        GRANT_PERMISSION, REVOKE_PERMISSION
    }

    public static UserDeviceEditPayload newAddUserPayload(UserDevice userDevice) {
        UserDeviceEditPayload payload = new UserDeviceEditPayload();
        payload.action = Action.ADD_USERDEVICE;
        payload.userDevice = userDevice;
        return payload;
    }

    public static UserDeviceEditPayload newRemoveUserPayload(UserDevice userDevice) {
        UserDeviceEditPayload payload = new UserDeviceEditPayload();
        payload.action = Action.REMOVE_USERDEVICE;
        payload.userDevice = userDevice;
        return payload;
    }

    public static UserDeviceEditPayload newEditUserPayload(UserDevice oldDevice, UserDevice newDevice) {
        UserDeviceEditPayload payload = new UserDeviceEditPayload();
        payload.action = Action.EDIT_USERDEVICE;
        payload.oldUserDevice = oldDevice;
        payload.newUserDevice = newDevice;
        return payload;
    }

    public static UserDeviceEditPayload newGrantPermissionPayload(UserDevice userDevice, Permission permission) {
        UserDeviceEditPayload payload = new UserDeviceEditPayload();
        payload.action = Action.GRANT_PERMISSION;
        payload.userDevice = userDevice;
        payload.permission = permission;
        return payload;
    }

    public static UserDeviceEditPayload newRevokePermissionPayload(UserDevice userDevice, Permission permission) {
        UserDeviceEditPayload payload = new UserDeviceEditPayload();
        payload.action = Action.REVOKE_PERMISSION;
        payload.userDevice = userDevice;
        payload.permission = permission;
        return payload;
    }

    public Action getAction() {
        return action;
    }

    public UserDevice getUserDeviceToAdd() {
        if (action != Action.ADD_USERDEVICE) {
            throw new IllegalStateException("Invalid action");
        }
        return userDevice;
    }

    public UserDevice getUserDeviceToRemove() {
        if (action != Action.REMOVE_USERDEVICE) {
            throw new IllegalStateException("Invalid action");
        }
        return userDevice;
    }

    public Map<UserDevice, Permission> getUserToRevokePermission() {
        if (action != Action.REVOKE_PERMISSION) {
            throw new IllegalStateException("Invalid action");
        }
        Map<UserDevice, Permission> map = new HashMap<>(1);
        map.put(userDevice, permission);
        return map;
    }

    public Map<UserDevice, Permission> getUserToGrantPermission() {
        if (action != Action.GRANT_PERMISSION) {
            throw new IllegalStateException("Invalid action");
        }
        Map<UserDevice, Permission> map = new HashMap<>(1);
        map.put(userDevice, permission);
        return map;
    }

    public Map<UserDevice, UserDevice> getUserToEdit() {
        if (action != Action.EDIT_USERDEVICE) {
            throw new IllegalStateException("Invalid action");
        }
        Map<UserDevice, UserDevice> map = new HashMap<>(1);
        map.put(oldUserDevice, newUserDevice);
        return map;
    }


}