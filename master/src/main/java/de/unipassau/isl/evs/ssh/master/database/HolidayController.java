package de.unipassau.isl.evs.ssh.master.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import com.google.common.base.Strings;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.HolidayAction;

/**
 * Offers high level methods to interact with the holiday table in the database.
 *
 * @author Leon Sell
 */
public class HolidayController extends AbstractComponent {
    public static final Key<HolidayController> KEY = new Key<>(HolidayController.class);
    private DatabaseConnector databaseConnector;
    // TODO: 06.01.16 Leon, create class containing all queries
    private static final String MODULE_ID_FROM_NAME_SQL_QUERY =
            "select " + DatabaseContract.ElectronicModule.COLUMN_ID
                    + " from " + DatabaseContract.ElectronicModule.TABLE_NAME
                    + " where " + DatabaseContract.ElectronicModule.COLUMN_NAME
                    + " = ?";

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
     * @param moduleName Module where the action occurs.
     */
    public void addHolidayLogEntryNow(String action, String moduleName) throws UnknownReferenceException {
        if (Strings.isNullOrEmpty(moduleName)) {
            databaseConnector.executeSql(
                    "insert into " + DatabaseContract.HolidayLog.TABLE_NAME
                            + " (" + DatabaseContract.HolidayLog.COLUMN_ACTION
                            + ", " + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP + ") values (?, ?)",
                    new String[] { action, String.valueOf(System.currentTimeMillis()) }
            );
        } else {
            try {
                databaseConnector.executeSql(
                        "insert into " + DatabaseContract.HolidayLog.TABLE_NAME
                                + " (" + DatabaseContract.HolidayLog.COLUMN_ACTION
                                + ", " + DatabaseContract.HolidayLog.COLUMN_ELECTRONIC_MODULE_ID
                                + ", " + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP + ") values (?,("
                                + MODULE_ID_FROM_NAME_SQL_QUERY + "),?)",
                        new String[]{ action, moduleName, String.valueOf(System.currentTimeMillis()) }
                );
            } catch (SQLiteConstraintException sqlce) {
                throw new UnknownReferenceException("The given module doesn't exist.", sqlce);
            }
        }
    }

    // TODO: 06.01.16 Leon, remove and update test.
    /**
     * Returns all actions logged and saved into the holiday table in a given range of time.
     *
     * @param from Start point in time of the range.
     * @param to   End point in time of the range.
     * @return List of the entries found.
     */
    @Deprecated
    public List<String> getLogEntriesRange(Date from, Date to) {
        Cursor holidayEntriesCursor = databaseConnector.executeSql("select "
                        + DatabaseContract.HolidayLog.COLUMN_ACTION
                        + ", " + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP
                        + " from " + DatabaseContract.HolidayLog.TABLE_NAME
                        + " where " + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP
                        + " >= ? and " + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP + " <= ?",
                new String[]{String.valueOf(from.getTime()), String.valueOf(to.getTime())});
        List<String> actions = new LinkedList<>();
        while (holidayEntriesCursor.moveToNext()) {
            actions.add(holidayEntriesCursor.getString(0));
        }
        return actions;
    }

    public List<HolidayAction> getHolidayActions(Date from, Date to) {
        Cursor holidayEntriesCursor = databaseConnector.executeSql("select "
                        + "h." + DatabaseContract.HolidayLog.COLUMN_ACTION
                        + ", m." + DatabaseContract.ElectronicModule.COLUMN_NAME
                        + ", h." + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP
                        + " from " + DatabaseContract.HolidayLog.TABLE_NAME + " h"
                        + " join " + DatabaseContract.ElectronicModule.TABLE_NAME + " m"
                        + " on h." + DatabaseContract.HolidayLog.COLUMN_ELECTRONIC_MODULE_ID
                        + " = m." + DatabaseContract.ElectronicModule.COLUMN_ID
                        + " where " + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP
                        + " >= ? and " + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP + " <= ?",
                new String[]{String.valueOf(from.getTime()), String.valueOf(to.getTime())});
        List<HolidayAction> actions = new LinkedList<>();
        while (holidayEntriesCursor.moveToNext()) {
            actions.add(new HolidayAction(
                            holidayEntriesCursor.getString(1),
                            holidayEntriesCursor.getLong(2),
                            holidayEntriesCursor.getString(0)
                    )
            );
        }
        return actions;
    }
}