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
