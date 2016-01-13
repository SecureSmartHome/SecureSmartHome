package de.unipassau.isl.evs.ssh.app.handler;

/**
 * TODO Wolfi: add javadoc (Phil, 2016-01-13)
 *
 * @author Wolfgang Popp.
 */
public class UserConfigurationEvent {
    private final boolean wasSuccessful;
    private EventType type;

    public UserConfigurationEvent(){
        this.wasSuccessful = true;
        this.type = EventType.PUSH;
    }

    public UserConfigurationEvent(EventType type, boolean wasSuccessful) {
        this.type = type;
        this.wasSuccessful = wasSuccessful;
    }

    public EventType getType() {
        return type;
    }

    public boolean wasSuccessful(){
        return wasSuccessful;
    }

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
