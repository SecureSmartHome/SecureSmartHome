package de.unipassau.isl.evs.ssh.master.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;

/**
 * The DatabaseConnector allows to establish connections to the used database and execute operations on it.
 */
public class DatabaseConnector extends AbstractComponent {
    public static final Key<DatabaseConnector> KEY = new Key<>(DatabaseConnector.class);

    private SQLiteDatabase db;

    /**
     * Execute the given sql statement on the database.
     *
     * @param sql           Sql statement to execute.
     * @param selectionArgs You may include ?s in the query, which will be replaced by the values from selectionArgs.
     * @return the cursor containing the data of the query
     */
    public synchronized Cursor executeSql(String sql, String[] selectionArgs) {
        Cursor result = null;
        db.beginTransaction();
        try {
            result = db.rawQuery(sql, selectionArgs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return result;
    }

    @Override
    public void init(Container container) {
        super.init(container);
        db = new DBOpenHelper(container.require(ContainerService.KEY_CONTEXT)).getWritableDatabase();
    }

    @Override
    public void destroy() {
        db.close();
        super.destroy();
    }

    public class DBOpenHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "SecureSmartHome.db";
        private static final String SQL_CREATE_DB = "CREATE TABLE " + DatabaseContract.UserDevice.TABLE_NAME + " ("
                + DatabaseContract.UserDevice.COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY,"
                + DatabaseContract.UserDevice.COLUMN_NAME + " VARCHAR NOT NULL UNIQUE,"
                + DatabaseContract.UserDevice.COLUMN_FINGERPRINT + " VARCHAR NOT NULL UNIQUE"
                + ");"

                + "CREATE TABLE " + DatabaseContract.Permission.TABLE_NAME + " ("
                + DatabaseContract.Permission.COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY,"
                + DatabaseContract.Permission.COLUMN_NAME + " VARCHAR NOT NULL UNIQUE"
                + ");"

                + "CREATE TABLE " + DatabaseContract.HasPermission.TABLE_NAME + " ("
                + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID + " INTEGER NOT NULL,"
                + DatabaseContract.HasPermission.COLUMN_USER_ID + " INTEGER NOT NULL,"
                + "PRIMARY KEY (permissionId, userId),"
                + "FOREIGN KEY(userId) REFERENCES UserDevice(_ID),"
                + "FOREIGN KEY(permissionId) REFERENCES Permission(_ID)"
                + ");"

                + "CREATE TABLE " + DatabaseContract.HolidayLog.TABLE_NAME + " ("
                + DatabaseContract.HolidayLog.COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY,"
                + DatabaseContract.HolidayLog.COLUMN_ACTION + " VARCHAR NOT NULL,"
                + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP + " INTEGER NOT NULL"
                + ");"

                + "CREATE TABLE" + DatabaseContract.Group.TABLE_NAME + " ("
                + DatabaseContract.Group.COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY,"
                + DatabaseContract.Group.COLUMN_NAME + " VARCHAR NOT NULL UNIQUE,"
                + DatabaseContract.Group.COLUMN_PERMISSION_TEMPLATE_ID + " INTEGER NOT NULL,"
                + "FOREIGN KEY(permissionTemplateId) REFERENCES PermissionTemplate(_ID)"
                + ");"

                + "CREATE TABLE" + DatabaseContract.MemberOf.TABLE_NAME + " ("
                + DatabaseContract.MemberOf.COLUMN_USER_ID + " INTEGER NOT NULL,"
                + DatabaseContract.MemberOf.COLUMN_GROUP_ID + " INTEGER NOT NULL,"
                + "PRIMARY KEY (userId, groupId),"
                + "FOREIGN KEY(groupId) REFERENCES 'Group'(_ID),"
                + "FOREIGN KEY(userId) REFERENCES UserDevice(_ID)"
                + ");"

                + "CREATE TABLE" + DatabaseContract.PermissionTemplate.TABLE_NAME + " ("
                + DatabaseContract.PermissionTemplate.COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY,"
                + DatabaseContract.PermissionTemplate.COLUMN_NAME + " VARCHAR NOT NULL UNIQUE"
                + ");"

                + "CREATE TABLE" + DatabaseContract.ComposedOfPermission.TABLE_NAME + " ("
                + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_ID + " INTEGER NOT NULL,"
                + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_TEMPLATE_ID + " INTEGER NOT NULL,"
                + "PRIMARY KEY (permissionId, permissionTemplateId),"
                + "FOREIGN KEY(permissionTemplateId) REFERENCES PermissionTemplate(_ID),"
                + "FOREIGN KEY(permissionId) REFERENCES Permission(_ID)"
                + ");"

                + "CREATE TABLE" + DatabaseContract.ElectronicModule.TABLE_NAME + " ("
                + DatabaseContract.ElectronicModule.COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY,"
                + DatabaseContract.ElectronicModule.COLUMN_SLAVE_ID + " INTEGER NOT NULL,"
                + DatabaseContract.ElectronicModule.COLUMN_NAME + " VARCHAR NOT NULL UNIQUE,"
                + DatabaseContract.ElectronicModule.COLUMN_GPIO_PIN + " INTEGER,"
                + DatabaseContract.ElectronicModule.COLUMN_USB_PORT + " INTEGER,"
                + DatabaseContract.ElectronicModule.COLUMN_WLAN_PORT + " INTEGER,"
                + DatabaseContract.ElectronicModule.COLUMN_WLAN_USERNAME + " INTEGER,"
                + DatabaseContract.ElectronicModule.COLUMN_WLAN_PASSWORD + " INTEGER,"
                + DatabaseContract.ElectronicModule.COLUMN_WLAN_IP + " INTEGER,"
                + DatabaseContract.ElectronicModule.COLUMN_TYPE + " INTEGER CHECK(type = 'GPIO' or type = 'USB' or type = 'WLAN'),"
                + "FOREIGN KEY(slaveId) REFERENCES Slave(_ID)"
                + ");"

                + "CREATE TABLE" + DatabaseContract.Slave.TABLE_NAME + " ("
                + DatabaseContract.Slave.COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY,"
                + DatabaseContract.Slave.COLUMN_NAME + " VARCHAR NOT NULL UNIQUE,"
                + DatabaseContract.Slave.COLUMN_FINGERPRINT + " VARCHAR NOT NULL UNIQUE"
                + " );";

        private static final String SQL_DROP_TABLES = "DROP TABLE " + DatabaseContract.UserDevice.TABLE_NAME + ";"
                + "DROP TABLE " + DatabaseContract.Permission.TABLE_NAME + ";"
                + "DROP TABLE " + DatabaseContract.HasPermission.TABLE_NAME + ";"
                + "DROP TABLE " + DatabaseContract.HolidayLog.TABLE_NAME + ";"
                + "DROP TABLE " + DatabaseContract.Group.TABLE_NAME + ";"
                + "DROP TABLE " + DatabaseContract.MemberOf.TABLE_NAME + ";"
                + "DROP TABLE " + DatabaseContract.PermissionTemplate.TABLE_NAME + ";"
                + "DROP TABLE " + DatabaseContract.ComposedOfPermission.TABLE_NAME + ";"
                + "DROP TABLE " + DatabaseContract.ElectronicModule.TABLE_NAME + ";"
                + "DROP TABLE " + DatabaseContract.Slave.TABLE_NAME + ";";

        public DBOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_DB);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Drops all tables and creates them again.
            db.execSQL(SQL_DROP_TABLES);
            onCreate(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }
}