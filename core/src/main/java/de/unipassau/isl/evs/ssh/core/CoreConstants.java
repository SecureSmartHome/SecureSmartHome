package de.unipassau.isl.evs.ssh.core;

/**
 * This Constants class provides constants needed by all modules.
 */
public class CoreConstants {
    /**
     * Name used for SharedPreferences file
     */
    public static final String FILE_SHARED_PREFS = "shared-preferences";
    /**
     * Default port used by netty
     */
    public static final int DEFAULT_PORT = 13131;
    /**
     * Preferred port used by netty
     */
    public static final String PREF_PORT = "PREF_PORT";
    /**
     * Preferred host used by netty, default is {@code null}
     */
    public static final String PREF_HOST = "PREF_HOST";
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
}
