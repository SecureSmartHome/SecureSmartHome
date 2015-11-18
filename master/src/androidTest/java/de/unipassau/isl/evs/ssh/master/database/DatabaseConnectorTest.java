package de.unipassau.isl.evs.ssh.master.database;

import android.content.Context;
import android.database.Cursor;
import android.test.InstrumentationTestCase;

import junit.framework.TestCase;

import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;

public class DatabaseConnectorTest extends InstrumentationTestCase {

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

        db.executeSql("insert into UserDevice values (?,?,?);", new String[]{ "1001", "bob", "fingerprint" });
        c = db.executeSql("select * from UserDevice where _ID = 1001;", null);
        c.moveToFirst();
        assertEquals(c.getString(c.getColumnIndex("name")), "bob");

        db.executeSql("delete from UserDevice where _ID = ?;", new String[]{ "1001" });
        c = db.executeSql("select * from UserDevice where _ID = 1001;", null);
        assertTrue(c.getCount() == 0);

    }
}