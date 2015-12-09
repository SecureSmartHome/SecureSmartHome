package de.unipassau.isl.evs.ssh.master.database;

/**
 * The DatabaseContract class contains constants that describe the database layout. These constants
 * are the names of tables and their columns.
 *
 * @author Wolfgang Popp
 */
public class DatabaseContract {
    public class UserDevice {
        public static final String TABLE_NAME = "UserDevice";
        public static final String COLUMN_ID = "_ID";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_FINGERPRINT = "fingerprint";
        public static final String COLUMN_GROUP_ID = "GroupId";
    }

    public class Permission {
        public static final String TABLE_NAME = "Permission";
        public static final String COLUMN_ID = "_ID";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_ELECTRONIC_MODULE_ID = "electronicModuleId";

        //Does not describe a table but constants for the PermissionTable entries
        public class Values {
            //Odroid
            public static final String ADD_ORDROID = "AddOdroid";
            public static final String RENAME_ORDROID = "RenameOdroid";
            public static final String DELETE_ORDROID = "DeleteOdroid";
            //Sensor
            public static final String ADD_SENSOR= "AddSensor";
            public static final String RENAME_MODULE = "RenameSensor";
            public static final String DELETE_SENSOR = "DeleteSensor";
            //Window
            public static final String REQUEST_WINDOW_STATUS = "RequestWindowStatus";
            //Light
            public static final String REQUEST_LIGHT_STATUS = "RequestLightStatus";
            public static final String SWITCH_LIGHT = "SwitchLight";
            //Door
            public static final String REQUEST_DOOR_STATUS = "RequestDoorStatus";
            public static final String LOCK_DOOR = "LockDoor";
            public static final String UNLATCH_DOOR = "UnlatchDoor";
            //Camera
            public static final String REQUEST_CAMERA_STATUS = "RequestCameraStatus";
            public static final String TAKE_CAMERA_PICTURE = "TakeCameraPicture";
            //WeaterStation
            public static final String REQUEST_WEATHER_STATUS = "RequestWeatherStatus";
            //HolidaySimulation
            public static final String START_HOLIDAY_SIMULATION = "StartHolidaySimulation";
            public static final String STOP_HOLIDAY_SIMULATION = "StopHolidaySimulation";
            //User
            public static final String ADD_USER = "AddUser";
            public static final String DELETE_USER = "DeleteUser";
            public static final String CHANGE_USER_NAME = "ChangeUserName";
            public static final String CHANGE_USER_GROUP = "ChangeUserGroup";
            public static final String GRANT_USER_RIGHT = "GrantUserRight";  //TODO rename to GRANT_USER_PERMISSION
            public static final String WITHDRAW_USER_RIGHT = "WithdrawUserRight"; //TODO rename to REVOKE_USER_PERMISSION
            //Groups
            public static final String ADD_GROUP = "AddGroup";
            public static final String DELETE_GROUP = "DeleteGroup";
            public static final String CHANGE_GROUP_NAME = "ChangeGroupName";
            public static final String SHOW_GROUP_MEMBER = "ShowGroupMembers";
            public static final String CHANGE_GROUP_TEMPLATE = "ChangeGroupTemplate";
            //Templates
            public static final String CREATE_TEMPLATE = "CreateTemplate";
            public static final String DELETE_TEMPLATE = "DeleteTemplate";
            public static final String EDIT_TEMPLATE = "EditTemplate";
            public static final String SHOW_TEMPLATE_PERMISSION = "ShowTemplatePermission";
        }
    }

    public class HasPermission {
        public static final String TABLE_NAME = "has_permission";
        public static final String COLUMN_PERMISSION_ID = "permissionId";
        public static final String COLUMN_USER_ID = "userId";
    }

    public class HolidayLog {
        public static final String TABLE_NAME = "HolidayLog";
        public static final String COLUMN_ID = "_ID";
        public static final String COLUMN_ACTION = "action";
        public static final String COLUMN_TIMESTAMP = "timestamp";
    }

    public class Group {
        public static final String TABLE_NAME = "DeviceGroup";
        public static final String COLUMN_ID = "_ID";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_PERMISSION_TEMPLATE_ID = "permissionTemplateId";
    }

    public class PermissionTemplate {
        public static final String TABLE_NAME = "PermissionTemplate";
        public static final String COLUMN_ID = "_ID";
        public static final String COLUMN_NAME = "name";
    }

    public class ComposedOfPermission {
        public static final String TABLE_NAME = "composed_of_permission";
        public static final String COLUMN_PERMISSION_ID = "permissionId";
        public static final String COLUMN_PERMISSION_TEMPLATE_ID = "permissionTemplateId";
    }

    public class ElectronicModule {
        public static final String TABLE_NAME = "ElectronicModule";
        public static final String COLUMN_MODULE_TYPE = "moduleType";
        public static final String COLUMN_ID = "_ID";
        public static final String COLUMN_SLAVE_ID = "slaveId";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_GPIO_PIN = "gpioPin";
        public static final String COLUMN_USB_PORT = "usbPort";
        public static final String COLUMN_WLAN_PORT = "wlanPort";
        public static final String COLUMN_WLAN_USERNAME = "wlanUsername";
        public static final String COLUMN_WLAN_PASSWORD = "wlanPassword";
        public static final String COLUMN_WLAN_IP = "wlanIP";
        public static final String COLUMN_CONNECTOR_TYPE = "type";
    }

    public class Slave {
        public static final String TABLE_NAME = "Slave";
        public static final String COLUMN_ID = "_ID";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_FINGERPRINT = "fingerprint";
    }
}
