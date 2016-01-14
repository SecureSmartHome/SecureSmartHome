package de.unipassau.isl.evs.ssh.master.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import com.google.common.base.Strings;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.UnknownReferenceException;
import de.unipassau.isl.evs.ssh.core.database.dto.HolidayAction;

/**
 * Offers high level methods to interact with the holiday table in the database.
 *
 * @author Leon Sell
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
     * @param action     Action to be added to the database.
     * @param moduleName Module where the action occurs.
     * @param timestamp  The timestamp of the action.
     */
    public void addHolidayLogEntry(String action, String moduleName, long timestamp) throws UnknownReferenceException {
        if (Strings.isNullOrEmpty(moduleName)) {
            databaseConnector.executeSql(
                    "insert into " + DatabaseContract.HolidayLog.TABLE_NAME
                            + " (" + DatabaseContract.HolidayLog.COLUMN_ACTION
                            + ", " + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP + ") values (?, ?)",
                    new String[]{action, String.valueOf(timestamp)}
            );
        } else {
            try {
                databaseConnector.executeSql(
                        "insert into " + DatabaseContract.HolidayLog.TABLE_NAME
                                + " (" + DatabaseContract.HolidayLog.COLUMN_ACTION
                                + ", " + DatabaseContract.HolidayLog.COLUMN_ELECTRONIC_MODULE_ID
                                + ", " + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP + ") values (?,("
                                + DatabaseContract.SqlQueries.MODULE_ID_FROM_NAME_SQL_QUERY + "),?)",
                        new String[]{action, moduleName, String.valueOf(timestamp)}
                );
            } catch (SQLiteConstraintException sqlce) {
                throw new UnknownReferenceException("The given module doesn't exist.", sqlce);
            }
        }
    }

    /**
     * Add a new action to the database. With the current time as the timestamp.
     *
     * @param action     Action to be added to the database.
     * @param moduleName Module where the action occurs.
     */
    public void addHolidayLogEntryNow(String action, String moduleName) throws UnknownReferenceException {
        addHolidayLogEntry(action, moduleName, System.currentTimeMillis());
    }

    /**
     * Returns all actions logged and saved into the holiday table in a given range of time.
     *
     * @param from Start point in time of the range.
     * @param to   End point in time of the range.
     * @return List of the entries found.
     */
    public List<HolidayAction> getHolidayActions(long from, long to) {
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
                new String[]{String.valueOf(from), String.valueOf(to)});
        List<HolidayAction> actions = new LinkedList<>();
        while (holidayEntriesCursor.moveToNext()) {
            actions.add(new HolidayAction(
                            holidayEntriesCursor.getString(1),
                            holidayEntriesCursor.getLong(2),
                            holidayEntriesCursor.getString(0)
                    )
            );
        }
        holidayEntriesCursor = databaseConnector.executeSql("select "
                        + DatabaseContract.HolidayLog.COLUMN_ACTION
                        + ", " + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP
                        + " from " + DatabaseContract.HolidayLog.TABLE_NAME
                        + " where " + DatabaseContract.HolidayLog.COLUMN_ELECTRONIC_MODULE_ID
                        + " is NULL"
                        + " and " + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP
                        + " >= ? and " + DatabaseContract.HolidayLog.COLUMN_TIMESTAMP + " <= ?",
                new String[]{String.valueOf(from), String.valueOf(to)});
        while (holidayEntriesCursor.moveToNext()) {
            actions.add(new HolidayAction(
                            null,
                            holidayEntriesCursor.getLong(1),
                            holidayEntriesCursor.getString(0)
                    )
            );
        }
        return actions;
    }
}