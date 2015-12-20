package de.unipassau.isl.evs.ssh.master.database;

import android.content.Context;
import android.database.Cursor;
import android.test.InstrumentationTestCase;

import de.unipassau.isl.evs.ssh.core.Permission;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;

/**
 * Instrumentation test for the DatabaseConnector.
 * Creates a database and tests connecting to it.
 *
 * @author Wolfgang Popp
 */
public class DatabaseConnectorTest extends InstrumentationTestCase {

    /**
     * Test method for this instrumentation test
     *
     * @throws Exception
     */
    public void testExecuteSql() throws Exception {
        SimpleContainer container = new SimpleContainer();
        Context context = getInstrumentation().getTargetContext();
        container.register(ContainerService.KEY_CONTEXT, new ContainerService.ContextComponent(context));

        context.deleteDatabase(DatabaseConnector.DBOpenHelper.DATABASE_NAME);
        container.register(DatabaseConnector.KEY, new DatabaseConnector());

        DatabaseConnector db = container.require(DatabaseConnector.KEY);
        Cursor c = db.executeSql("select name from sqlite_master where type = 'table' and name = 'HolidayLog'", null);
        c.moveToFirst();
        assertEquals(c.getString(c.getColumnIndex("name")), "HolidayLog");

        db.executeSql("insert into PermissionTemplate values (123, 'theTemplate')", null);
        db.executeSql("insert into DeviceGroup values (?,?,?)", new String[]{"2", "theGroup", "123"});
        db.executeSql("insert into UserDevice values (?,?,?,?);", new String[]{"1001", "bob", "fingerprint", "2"});
        c = db.executeSql("select * from UserDevice where _ID = 1001;", null);
        c.moveToFirst();
        assertEquals(c.getString(c.getColumnIndex("name")), "bob");

        db.executeSql("delete from UserDevice where _ID = ?;", new String[]{"1001"});
        c = db.executeSql("select * from UserDevice where _ID = 1001;", null);
        assertTrue(c.getCount() == 0);

        c = db.executeSql("Select " + DatabaseContract.Permission.COLUMN_NAME + " from " + DatabaseContract.Permission.TABLE_NAME, null);
        while (c.moveToNext()) {
            c.getString(c.getColumnIndex(DatabaseContract.Permission.COLUMN_NAME));
        }
        c = db.executeSql("Select " + DatabaseContract.Permission.COLUMN_NAME + " from " + DatabaseContract.Permission.TABLE_NAME + " where _ID = 1", null);
        c.moveToFirst();
        assertEquals(Permission.ADD_ODROID.toString(), c.getString(c.getColumnIndex(DatabaseContract.Permission.COLUMN_NAME)));

    }
}