package de.unipassau.isl.evs.ssh.master.database;

import java.util.Date;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;

/**
 * Offers high level methods to interact with the holiday table in the database.
 */
public class HolidayController extends AbstractComponent {
    public static final Key<HolidayController> KEY = new Key<>(HolidayController.class);

    /**
     * Add a new action to the database.
     *
     * @param action Action to be added to the database.
     */
    public void addHolidayLogEntry(String action) {
        // TODO - implement HolidayController.addHolidayLogEntry
        throw new UnsupportedOperationException();
    }

    /**
     * Returns all actions logged and saved into the holiday table in a given range of time.
     *
     * @param from Start point in time of the range.
     * @param to   End point in time of the range.
     * @return List of the entries found.
     */
    public List<String> getLogEntriesRange(Date from, Date to) {
        // TODO - implement HolidayController.getLogEntriesRange
        throw new UnsupportedOperationException();
    }

}