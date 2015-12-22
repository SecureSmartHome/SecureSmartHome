package de.unipassau.isl.evs.ssh.core.sec;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.CoreConstants;

/**
 * @author Team
 */
public enum Permission {
    //Odroid
    ADD_ODROID,
    RENAME_ODROID,
    DELETE_ODROID,

    //Sensor
    ADD_SENSOR,
    RENAME_MODULE,
    DELETE_SENSOR,

    //Light
    REQUEST_LIGHT_STATUS,

    // Window
    REQUEST_WINDOW_STATUS,

    //Door
    REQUEST_DOOR_STATUS,
    LOCK_DOOR,
    UNLATCH_DOOR,

    //Camera
    REQUEST_CAMERA_STATUS,
    TAKE_CAMERA_PICTURE,

    //WeaterStation
    REQUEST_WEATHER_STATUS,

    //HolidaySimulation
    START_HOLIDAY_SIMULATION,
    STOP_HOLIDAY_SIMULATION,

    //User
    ADD_USER,
    DELETE_USER,
    CHANGE_USER_NAME,
    CHANGE_USER_GROUP,
    GRANT_USER_PERMISSION,
    WITHDRAW_USER_PERMISSION,

    //Groups
    ADD_GROUP,
    DELETE_GROUP,
    CHANGE_GROUP_NAME,
    SHOW_GROUP_MEMBER,
    CHANGE_GROUP_TEMPLATE,

    //Templates
    CREATE_TEMPLATE,
    DELETE_TEMPLATE,
    EDIT_TEMPLATE,
    SHOW_TEMPLATE_PERMISSION,

    //Notification Types
    ODROID_ADDED,
    HUMIDITY_WARNING,
    BRIGHTNESS_WARNING,
    HOLIDAY_MODE_SWITCHED_ON,
    HOLIDAY_MODE_SWITCHED_OFF,
    SYSTEM_HEALTH_WARNING,
    BELL_RANG,
    WEATHER_WARNING,
    DOOR_UNLATCHED,
    DOOR_LOCKED,
    DOOR_UNLOCKED,

    //Ternary Permissions
    SWITCH_LIGHT(true);

    private final boolean isTernary;

    Permission() {
        this(false);
    }

    Permission(boolean isTernary) {
        this.isTernary = isTernary;
    }

    public boolean isTernary() {
        return isTernary;
    }

    /**
     * @deprecated use {@link #getPermissions(CoreConstants.ModuleType)} instead
     */
    @Deprecated
    public static Permission[] getPermissions(String moduleType) {
        return getPermissions(CoreConstants.ModuleType.valueOf(moduleType));
    }

    public static Permission[] getPermissions(CoreConstants.ModuleType moduleType) {
        switch (moduleType) {
            case Light:
                return new Permission[]{SWITCH_LIGHT};
            default:
                return null;
        }
    }

    public static final List<Permission> binaryPermissions = filter(new Predicate<Permission>() {
        @Override
        public boolean apply(Permission input) {
            return !input.isTernary();
        }
    });

    public static final List<Permission> ternaryPermissions = filter(new Predicate<Permission>() {
        @Override
        public boolean apply(Permission input) {
            return input.isTernary();
        }
    });

    private static List<Permission> filter(Predicate<Permission> predicate) {
        final ArrayList<Permission> list = new ArrayList<>();
        final Iterable<Permission> iterable = Iterables.filter(Arrays.asList(values()), predicate);
        Iterables.addAll(list, iterable);
        return Collections.unmodifiableList(list);
    }
}
