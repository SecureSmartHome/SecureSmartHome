package de.unipassau.isl.evs.ssh.app.handler;

/**
 * The UserConfigurationEvent indicates that an user configuration action occurred. These action occur when properties
 * of a user change, e.g. the name of a user changed.
 *
 * @author Wolfgang Popp.
 */
public class UserConfigurationEvent {
    private final boolean wasSuccessful;
    private final EventType type;

    /**
     * Constructs a new UserConfigurationEvent for a pushed message. Pushed messages are messages that are not a reply
     * to a former request.
     */
    public UserConfigurationEvent() {
        this.wasSuccessful = true;
        this.type = EventType.PUSH;
    }

    /**
     * Constructs a new UserConfigurationEvent.
     *
     * @param type          the type of action that triggered this event
     * @param wasSuccessful true if the reply to a former request was no error
     */
    public UserConfigurationEvent(EventType type, boolean wasSuccessful) {
        this.type = type;
        this.wasSuccessful = wasSuccessful;
    }

    /**
     * Gets the type of this event.
     *
     * @return the type of this event
     */
    public EventType getType() {
        return type;
    }

    /**
     * Checks weather the action corresponding to this event was successful.
     *
     * @return true if the actino was successful
     */
    public boolean wasSuccessful() {
        return wasSuccessful;
    }

    /**
     * The EventType enum lists possible event types that occur when changing the user configuration.
     */
    public enum EventType {
        PUSH,
        PERMISSION_GRANT,
        PERMISSION_REVOKE,
        USERNAME_SET,
        USER_SET_GROUP,
        USER_DELETE,
        GROUP_ADD,
        GROUP_DELETE,
        GROUP_SET_NAME,
        GROUP_SET_TEMPLATE
    }
}
