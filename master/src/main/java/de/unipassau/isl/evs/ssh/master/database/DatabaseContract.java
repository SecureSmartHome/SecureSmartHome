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

/**
 * The DatabaseContract class contains constants that describe the database layout. These constants
 * are the names of tables and their columns.
 *
 * @author Wolfgang Popp
 */
public class DatabaseContract {

    public static class Group {
        public static final String TABLE_NAME = "DeviceGroup";
        public static final String COLUMN_ID = "_ID";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_PERMISSION_TEMPLATE_ID = "permissionTemplateId";

        public enum DefaultValues {
            PARENTS("Parents"),
            CHILDREN("Children"),
            GUESTS("Guests");

            final String name;

            DefaultValues(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                return name;
            }
        }
    }

    public static class PermissionTemplate {
        public static final String TABLE_NAME = "PermissionTemplate";
        public static final String COLUMN_ID = "_ID";
        public static final String COLUMN_NAME = "name";

        public enum DefaultValues {
            PARENTS_TEMPLATE("ParentsTemplate"),
            CHILDREN_TEMPLATE("ChildrenTemplate"),
            GUESTS_TEMPLATE("GuestsTemplate");

            final String name;

            DefaultValues(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                return name;
            }
        }
    }

    /**
     * @author Leon Sell
     */
    public class SqlQueries {
        static final String SLAVE_ID_FROM_FINGERPRINT_SQL_QUERY =
                "select " + DatabaseContract.Slave.COLUMN_ID
                        + " from " + DatabaseContract.Slave.TABLE_NAME
                        + " where " + DatabaseContract.Slave.COLUMN_FINGERPRINT
                        + " = ?";
        static final String GROUP_ID_FROM_NAME_SQL_QUERY =
                "select " + DatabaseContract.Group.COLUMN_ID
                        + " from " + DatabaseContract.Group.TABLE_NAME
                        + " where " + DatabaseContract.Group.COLUMN_NAME
                        + " = ?";
        static final String PERMISSION_ID_FROM_NAME_AND_MODULE_SQL_QUERY =
                "select p." + DatabaseContract.Permission.COLUMN_ID
                        + " from " + DatabaseContract.Permission.TABLE_NAME + " p"
                        + " join " + DatabaseContract.ElectronicModule.TABLE_NAME + " m"
                        + " on p." + DatabaseContract.Permission.COLUMN_ELECTRONIC_MODULE_ID
                        + " = m." + DatabaseContract.ElectronicModule.COLUMN_ID
                        + " where p." + DatabaseContract.Permission.COLUMN_NAME
                        + " = ? and m." + DatabaseContract.ElectronicModule.COLUMN_NAME
                        + " = ?";
        static final String PERMISSION_ID_FROM_NAME_WITHOUT_MODULE_SQL_QUERY =
                "select " + DatabaseContract.Permission.COLUMN_ID
                        + " from " + DatabaseContract.Permission.TABLE_NAME
                        + " where " + DatabaseContract.Permission.COLUMN_NAME
                        + " = ? and " + DatabaseContract.Permission.COLUMN_ELECTRONIC_MODULE_ID + " is NULL";
        static final String TEMPLATE_ID_FROM_NAME_SQL_QUERY =
                "select " + DatabaseContract.PermissionTemplate.COLUMN_ID
                        + " from " + DatabaseContract.PermissionTemplate.TABLE_NAME
                        + " where " + DatabaseContract.PermissionTemplate.COLUMN_NAME
                        + " = ?";
        static final String USER_DEVICE_ID_FROM_FINGERPRINT_SQL_QUERY =
                "select " + DatabaseContract.UserDevice.COLUMN_ID
                        + " from " + DatabaseContract.UserDevice.TABLE_NAME
                        + " where " + DatabaseContract.UserDevice.COLUMN_FINGERPRINT
                        + " = ?";
        static final String MODULE_ID_FROM_NAME_SQL_QUERY =
                "select " + DatabaseContract.ElectronicModule.COLUMN_ID
                        + " from " + DatabaseContract.ElectronicModule.TABLE_NAME
                        + " where " + DatabaseContract.ElectronicModule.COLUMN_NAME
                        + " = ?";
    }

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
        public static final String COLUMN_ELECTRONIC_MODULE_ID = "electronicModuleId";
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
