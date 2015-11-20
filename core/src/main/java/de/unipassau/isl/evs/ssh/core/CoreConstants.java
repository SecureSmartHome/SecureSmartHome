package de.unipassau.isl.evs.ssh.core;

/**
 * This Constants class provides constants needed by all modules.
 */
public class CoreConstants {
    /**
     * Name used for SharedPreferences file
     */
    public static final String FILE_SHARED_PREFS = "ssh.core.shared-preferences";
    /**
     * Default port used by netty
     */
    public static final int DEFAULT_PORT = 13131;
    /**
     * Preferred port used by netty
     */
    public static final String PREF_PORT = "ssh.core.PREF_PORT";
    /**
     * Preferred host used by netty, default is {@code null}
     */
    public static final String PREF_HOST = "ssh.core.PREF_HOST";
    /**
     * The time it takes in seconds after the client starts idle when reader isn't active.
     */
    public static final int CLIENT_READER_IDLE_TIME = 60;
    /**
     * The time it takes in seconds after the client starts idle when writer isn't active.
     */
    public static final int CLIENT_WRITER_IDLE_TIME = 30;
    /**
     * The time it takes in seconds after the client starts idle when reader or writer isn't active.
     * Set to infinite.
     */
    public static final int CLIENT_ALL_IDLE_TIME = 0;
    /**
     * Default address used for UDP broadcasts.
     */
    public static final String BROADCAST_ADDRESS = "ssh.core.BROADCAST_ADDRESS";
    /**
     * Default value for timeouts. Set to zero.
     */
    public static final int DEFAULT_TIMEOUTS = 0;
    /**
     * Default value for maximum timeouts.
     */
    public static final int MAX_NUMBER_OF_TIMEOUTS = 3;
    /**
     * The minimum number of seconds between
     */
    public static final int MIN_SECONDS_BETWEEN_TIMEOUTS = 60;
    /**
     * The maximum number of seconds the broadcast waits to be sent again.
     */
    public static final int MAX_SECONDS_BETWEEN_BROADCAST = 10;
    /**
     * Number of how many timeouts occurred in a row.
     */
    public static final String TIMEOUTS_IN_A_ROW = "ssh.core.TIMEOUTS_IN_A_ROW";
}
