package de.unipassau.isl.evs.ssh.master.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.GPIOAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.ModuleAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.USBAccessPoint;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.WLANAccessPoint;

/**
 * The DatabaseConnector allows to establish connections to the used database and execute operations on it.
 *
 * @author Wolfgang Popp
 */
public class DatabaseConnector extends AbstractComponent {
    public static final Key<DatabaseConnector> KEY = new Key<>(DatabaseConnector.class);
    public static final String TAG = DatabaseConnector.class.getSimpleName();

    private SQLiteDatabase database;

    /**
     * Execute the given sql statement on the database.
     *
     * @param sql           Sql statement to execute.
     * @param selectionArgs You may include ?s in the query, which will be replaced by the values from selectionArgs.
     * @return the cursor containing the data of the query
     */
    public synchronized Cursor executeSql(String sql, String[] selectionArgs) {
        Cursor result = null;
        database.beginTransaction();
        try {
            //Log.v(TAG, "executing query: " + sql);
            result = database.rawQuery(sql, selectionArgs);
            // rawQuery() does not execute insert statements but only compiles them.
            // Cursor.moveTo...() executes the actual sql statement.
            result.moveToPosition(-1);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        return result;
    }

    @Override
    public void init(Container container) {
        Log.v(TAG, "init:called");
        super.init(container);
        database = new DBOpenHelper(container.require(ContainerService.KEY_CONTEXT)).getWritableDatabase();
        Log.v(TAG, "init:finished");
    }

    @Override
    public void destroy() {
        Log.v(TAG, "destroy:called");
        database.close();
        super.destroy();
        Log.v(TAG, "destroy:finished");
    }

    public class DBOpenHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 2;
        public static final String DATABASE_NAME = "SecureSmartHome.db";
        private static final String SQL_CREATE_DB = "CREATE TABLE " + DatabaseContract.UserDevice.TABLE_NAME + " ("
                + DatabaseContract.UserDevice.COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY,"
                + DatabaseContract.UserDevice.COLUMN_NAME + " VARCHAR NOT NULL UNIQUE,"
                + DatabaseContract.UserDevice.COLUMN_FINGERPRINT + " VARCHAR NOT NULL UNIQUE,"
                + DatabaseContract.UserDevice.COLUMN_GROUP_ID + " INTEGER NOT NULL,"
                + "FOREIGN KEY(" + DatabaseContract.UserDevice.COLUMN_GROUP_ID + ") REFERENCES " + DatabaseContract.Group.TABLE_NAME + "(" + DatabaseContract.Group.COLUMN_ID + ")"
                + ");"

                + "CREATE TABLE " + DatabaseContract.Permission.TABLE_NAME + " ("
                + DatabaseContract.Permission.COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY,"
                + DatabaseContract.Permission.COLUMN_NAME + " VARCHAR NOT NULL,"
                + DatabaseContract.Permission.COLUMN_ELECTRONIC_MODULE_ID + " INTEGER,"
                + "UNIQUE(" + DatabaseContract.Permission.COLUMN_NAME + ", " + DatabaseContract.Permission.COLUMN_ELECTRONIC_MODULE_ID + "),"
                + "FOREIGN KEY(" + DatabaseContract.Permission.COLUMN_ELECTRONIC_MODULE_ID + ") REFERENCES " + DatabaseContract.ElectronicModule.TABLE_NAME + "(" + DatabaseContract.ElectronicModule.COLUMN_ID + ") ON DELETE CASCADE"
                + ");"

                + "CREATE TABLE " + DatabaseContract.HasPermission.TABLE_NAME + " ("
                + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID + " INTEGER NOT NULL,"
                + DatabaseContract.HasPermission.COLUMN_USER_ID + " INTEGER NOT NULL,"
                + "PRIMARY KEY (" + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID + ", " + DatabaseContract.HasPermission.COLUMN_USER_ID + "),"
                + "FOREIGN KEY(" + DatabaseContract.HasPermission.COLUMN_USER_ID + ") REFERENCES " + DatabaseContract.UserDevice.TABLE_NAME + "(" + DatabaseContract.UserDevice.COLUMN_ID + ") ON DELETE CASCADE,"
                + "FOREIGN KEY(" + DatabaseContract.HasPermission.COLUMN_PERMISSION_ID + ") REFERENCES " + DatabaseContract.Permission.TABLE_NAME + "(" + DatabaseContract.Permission.COLUMN_ID + ") ON DELETE CASCADE"
                + ");"

                + "CREATE TABLE " + DatabaseContract.Group.TABLE_NAME + " ("
                + DatabaseContract.Group.COLUMN_ID + "  INTEGER NOT NULL PRIMARY KEY,"
                + DatabaseContract.Group.COLUMN_NAME + " VARCHAR NOT NULL UNIQUE,"
                + DatabaseContract.Group.COLUMN_PERMISSION_TEMPLATE_ID + " INTEGER NOT NULL,"
                + "FOREIGN KEY(" + DatabaseContract.Group.COLUMN_PERMISSION_TEMPLATE_ID + ") REFERENCES " + DatabaseContract.PermissionTemplate.TABLE_NAME + "(" + DatabaseContract.PermissionTemplate.COLUMN_ID + ")"
                + ");"

                + "CREATE TABLE " + DatabaseContract.PermissionTemplate.TABLE_NAME + " ("
                + DatabaseContract.PermissionTemplate.COLUMN_ID + "  INTEGER NOT NULL PRIMARY KEY,"
                + DatabaseContract.PermissionTemplate.COLUMN_NAME + " VARCHAR NOT NULL UNIQUE"
                + ");"

                + "CREATE TABLE " + DatabaseContract.ComposedOfPermission.TABLE_NAME + " ("
                + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_ID + " INTEGER NOT NULL,"
                + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_TEMPLATE_ID + " INTEGER NOT NULL,"
                + "PRIMARY KEY (" + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_ID + "," + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_TEMPLATE_ID + "),"
                + "FOREIGN KEY(" + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_TEMPLATE_ID + ") REFERENCES " + DatabaseContract.PermissionTemplate.TABLE_NAME + "(" + DatabaseContract.PermissionTemplate.COLUMN_ID + ") ON DELETE CASCADE,"
                + "FOREIGN KEY(" + DatabaseContract.ComposedOfPermission.COLUMN_PERMISSION_ID + ") REFERENCES " + DatabaseContract.Permission.TABLE_NAME + "(" + DatabaseContract.Permission.COLUMN_ID + ") ON DELETE CASCADE"
                + ");"

                + "CREATE TABLE " + DatabaseContract.ElectronicModule.TABLE_NAME + " ("
                + DatabaseContract.ElectronicModule.COLUMN_ID + "  INTEGER NOT NULL PRIMARY KEY,"
                + DatabaseContract.ElectronicModule.COLUMN_SLAVE_ID + " INTEGER NOT NULL,"
                + DatabaseContract.ElectronicModule.COLUMN_NAME + " VARCHAR NOT NULL UNIQUE,"
                + DatabaseContract.ElectronicModule.COLUMN_GPIO_PIN + " INTEGER,"
                + DatabaseContract.ElectronicModule.COLUMN_USB_PORT + " INTEGER,"
                + DatabaseContract.ElectronicModule.COLUMN_WLAN_PORT + " INTEGER,"
                + DatabaseContract.ElectronicModule.COLUMN_WLAN_USERNAME + " VARCHAR,"
                + DatabaseContract.ElectronicModule.COLUMN_WLAN_PASSWORD + " VARCHAR,"
                + DatabaseContract.ElectronicModule.COLUMN_WLAN_IP + " VARCHAR,"
                + DatabaseContract.ElectronicModule.COLUMN_MODULE_TYPE + " VARCHAR NOT NULL,"
                + DatabaseContract.ElectronicModule.COLUMN_CONNECTOR_TYPE + " VARCHAR CHECK("
                + DatabaseContract.ElectronicModule.COLUMN_CONNECTOR_TYPE + " = '" + GPIOAccessPoint.TYPE + "' or "
                + DatabaseContract.ElectronicModule.COLUMN_CONNECTOR_TYPE + " = '" + USBAccessPoint.TYPE + "' or "
                + DatabaseContract.ElectronicModule.COLUMN_CONNECTOR_TYPE + " = '" + WLANAccessPoint.TYPE + "'),"
                + "FOREIGN KEY(" + DatabaseContract.ElectronicModule.COLUMN_SLAVE_ID + ") REFERENCES " + DatabaseContract.Slave.TABLE_NAME + "(" + DatabaseContract.Slave.COLUMN_ID + ")"
                + ");"

                + "CREATE TABLE " + DatabaseContract.Slave.TABLE_NAME + " ("
                + DatabaseContract.Slave.COLUMN_ID + "  INTEGER NOT NULL PRIMARY KEY,"
                + DatabaseContract.Slave.COLUMN_NAME + " VARCHAR NOT NULL UNIQUE,"
                + DatabaseContract.Slave.COLUMN_FINGERPRINT + " VARCHAR NOT NULL UNIQUE"
                + ");"

                + "CREATE TABLE " + DatabaseContract.HolidayLog.TABLE_NAME + " ("
                + DatabaseContract.HolidayLog.COLUMN_ID + "  INTEGER NOT NULL PRIMARY KEY,"
                + DatabaseContract.HolidayLog.COLUMN_ACTION + " VARCHAR NOT NULL,"
                + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP + " INTEGER NOT NULL"
                + ");";

        private static final String SQL_DROP_TABLES = "DROP TABLE " + DatabaseContract.HasPermission.TABLE_NAME + ";"
                + "DROP TABLE " + DatabaseContract.ComposedOfPermission.TABLE_NAME + ";"
                + "DROP TABLE " + DatabaseContract.UserDevice.TABLE_NAME + ";"
                + "DROP TABLE " + DatabaseContract.Permission.TABLE_NAME + ";"
                + "DROP TABLE " + DatabaseContract.ElectronicModule.TABLE_NAME + ";"
                + "DROP TABLE " + DatabaseContract.Group.TABLE_NAME + ";"
                + "DROP TABLE " + DatabaseContract.Slave.TABLE_NAME + ";"
                + "DROP TABLE " + DatabaseContract.PermissionTemplate.TABLE_NAME + ";"
                + "DROP TABLE " + DatabaseContract.HolidayLog.TABLE_NAME + ";";

        private void insertPermissions(SQLiteDatabase db) {
            String[] binaryPermissions = new String[]{
                    DatabaseContract.Permission.Values.ADD_ORDROID,
                    DatabaseContract.Permission.Values.RENAME_ORDROID,
                    DatabaseContract.Permission.Values.DELETE_ORDROID,
                    DatabaseContract.Permission.Values.ADD_SENSOR,
                    DatabaseContract.Permission.Values.RENAME_SENSOR,
                    DatabaseContract.Permission.Values.DELETE_SENSOR,
                    DatabaseContract.Permission.Values.START_HOLIDAY_SIMULATION,
                    DatabaseContract.Permission.Values.STOP_HOLIDAY_SIMULATION,
                    DatabaseContract.Permission.Values.ADD_USER,
                    DatabaseContract.Permission.Values.DELETE_USER,
                    DatabaseContract.Permission.Values.CHANGE_USER_NAME,
                    DatabaseContract.Permission.Values.CHANGE_USER_GROUP,
                    DatabaseContract.Permission.Values.GRANT_USER_RIGHT,
                    DatabaseContract.Permission.Values.WITHDRAW_USER_RIGHT,
                    DatabaseContract.Permission.Values.ADD_GROUP,
                    DatabaseContract.Permission.Values.DELETE_GROUP,
                    DatabaseContract.Permission.Values.CHANGE_GROUP_NAME,
                    DatabaseContract.Permission.Values.SHOW_GROUP_MEMBER,
                    DatabaseContract.Permission.Values.CHANGE_GROUP_TEMPLATE,
                    DatabaseContract.Permission.Values.CREATE_TEMPLATE,
                    DatabaseContract.Permission.Values.DELETE_TEMPLATE,
                    DatabaseContract.Permission.Values.EDIT_TEMPLATE,
                    DatabaseContract.Permission.Values.SHOW_TEMPLATE_PERMISSION,
                    CoreConstants.NotificationTypes.ODROID_ADDED,
                    CoreConstants.NotificationTypes.HUMIDITY_WARNING,
                    CoreConstants.NotificationTypes.BRIGHTNESS_WARNING,
                    CoreConstants.NotificationTypes.HOLIDAY_MODE_SWITCHED_ON,
                    CoreConstants.NotificationTypes.HOLIDAY_MODE_SWITCHED_OFF,
                    CoreConstants.NotificationTypes.SYSTEM_HEALT_WARNING,
                    CoreConstants.NotificationTypes.BELL_RANG,
                    CoreConstants.NotificationTypes.WEATHER_WARNING,
                    CoreConstants.NotificationTypes.DOOR_UNLATCHED,
                    CoreConstants.NotificationTypes.DOOR_LOCKED,
                    CoreConstants.NotificationTypes.DOOR_UNLOCKED
            };

            for (String permission : binaryPermissions) {
                ContentValues values = new ContentValues(1);
                values.put(DatabaseContract.Permission.COLUMN_NAME, permission);
                db.insert(DatabaseContract.Permission.TABLE_NAME, null, values);
            }
        }

        private void execSQLScript(String script, SQLiteDatabase db) {
            String[] statements = script.split("\\;");
            for (String statement : statements) {
                //Log.v(TAG, "executing SQL statement: " + statement + ";");
                db.execSQL(statement + ";");
            }
        }

        public DBOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.v(TAG, "creating Database");
            execSQLScript(SQL_CREATE_DB, db);
            insertPermissions(db);

        }

        @Override
        public void onConfigure(SQLiteDatabase db) {
            db.setForeignKeyConstraintsEnabled(true);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Drops all tables and creates them again.
            Log.v(TAG, "updating Database");
            execSQLScript(SQL_DROP_TABLES, db);
            onCreate(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }
}