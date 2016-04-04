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

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.test.InstrumentationTestCase;

import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;
import de.unipassau.isl.evs.ssh.core.sec.Permission;

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

        context.deleteDatabase(DatabaseConnector.DATABASE_NAME);
        container.register(DatabaseConnector.KEY, new DatabaseConnector());

        DatabaseConnector db = container.require(DatabaseConnector.KEY);
        Cursor c = db.executeSql("select name from sqlite_master where type = 'table' and name = 'HolidayLog'", null);
        c.moveToFirst();
        assertEquals(c.getString(c.getColumnIndex("name")), "HolidayLog");

        c = db.executeSql("select name from " + DatabaseContract.Permission.TABLE_NAME + " where name = ?;", new String[] {Permission.TAKE_CAMERA_PICTURE.toString()});
        c.moveToFirst();
        assertEquals(c.getString(0), Permission.TAKE_CAMERA_PICTURE.toString());

        c = db.executeSql("select name from " + DatabaseContract.Group.TABLE_NAME + " where name = '" + DatabaseContract.Group.DefaultValues.CHILDREN.toString() + "';", null);
        c.moveToFirst();
        assertEquals(c.getString(0), DatabaseContract.Group.DefaultValues.CHILDREN.toString());

        db.executeSql("insert into PermissionTemplate values (123, 'theTemplate')", null);
        db.executeSql("insert into DeviceGroup values (?,?,?)", new String[]{"4", "theGroup", "123"});
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
    }
}