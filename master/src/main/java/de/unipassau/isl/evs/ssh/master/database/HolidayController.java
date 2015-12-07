package de.unipassau.isl.evs.ssh.master.database;

import android.database.Cursor;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.HolidayAction;

/**
 * Offers high level methods to interact with the holiday table in the database.
 * @author leon
 */
public class HolidayController extends AbstractComponent {
    public static final Key<HolidayController> KEY = new Key<>(HolidayController.class);
    private DatabaseConnector databaseConnector;

    @Override
    public void init(Container container) {
        super.init(container);
        databaseConnector = requireComponent(DatabaseConnector.KEY);
    }

    @Override
    public void destroy() {
        super.destroy();
        databaseConnector = null;
    }

    /**
     * Add a new action to the database.
     *
     * @param action Action to be added to the database.
     */
    public void addHolidayLogEntry(String action) {
        databaseConnector.executeSql("insert into " + DatabaseContract.HolidayLog.TABLE_NAME
                + " (" + DatabaseContract.HolidayLog.COLUMN_ACTION
                + ", " + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP + ") values (? ,?)",
                    new String[] { action, String.valueOf(System.currentTimeMillis()) });
    }

    /**
     * Returns all actions logged and saved into the holiday table in a given range of time.
     *
     * @param from Start point in time of the range.
     * @param to   End point in time of the range.
     * @return List of the entries found.
     */
    public List<String> getLogEntriesRange(Date from, Date to) {
        Cursor holidayEntriesCursor = databaseConnector.executeSql("select "
                + DatabaseContract.HolidayLog.COLUMN_ACTION
                + ", " + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP
                + " from " + DatabaseContract.HolidayLog.TABLE_NAME
                + " where " + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP
                + " >= ? and " + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP + " <= ?",
                    new String[] { String.valueOf(from.getTime()), String.valueOf(to.getTime()) });
        List<String> actions = new LinkedList<>();
        while (holidayEntriesCursor.moveToNext()) {
            actions.add(holidayEntriesCursor.getString(0));
        }
        return actions;
    }

    public List<HolidayAction> getHolidayActions(Date from, Date to) {
        return new LinkedList();
    }
}