package de.unipassau.isl.evs.ssh.master.database;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;

/**
 * The DatabaseConnector allows to establish connections to the used database and execute operations on it.
 */
public class DatabaseConnector extends AbstractComponent {
    public static final Key<DatabaseConnector> KEY = new Key<>(DatabaseConnector.class);

    /**
     * Execute the given sql statement on the database.
     *
     * @param sql Sql statement to execute.
     */
    public void executeSql(String sql) {
        // TODO - implement DatabaseConnector.executeSql
        throw new UnsupportedOperationException();
    }

}