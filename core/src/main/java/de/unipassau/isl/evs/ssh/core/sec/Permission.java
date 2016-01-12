package de.unipassau.isl.evs.ssh.core.sec;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.unipassau.isl.evs.ssh.core.CoreConstants;

/**
 * All Permissions that can be granted to a UserDevice and which a required for executing certain actions,
 * displaying certain information or receiving certain notifications.
 * TODO Niko JavaDoc
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
    UNLATCH_DOOR_ON_HOLIDAY,

    //Camera
    REQUEST_CAMERA_STATUS,
    TAKE_CAMERA_PICTURE,

    //WeatherStation
    REQUEST_WEATHER_STATUS,

    //HolidaySimulation
    TOGGLE_HOLIDAY_SIMULATION,

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

    /**
     * A ternary Permission can selectively be granted for certain modules.
     */
    private final boolean isTernary;
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

    Permission() {
        this(false);
    }

    Permission(boolean isTernary) {
        this.isTernary = isTernary;
    }

    @Nullable
    public static Permission[] getPermissions(CoreConstants.ModuleType moduleType) {
        switch (moduleType) {
            case Light:
                return new Permission[]{SWITCH_LIGHT};
            default:
                return null;
        }
    }

    private static List<Permission> filter(Predicate<Permission> predicate) {
        final ArrayList<Permission> list = new ArrayList<>();
        final Iterable<Permission> iterable = Iterables.filter(Arrays.asList(values()), predicate);
        Iterables.addAll(list, iterable);
        return Collections.unmodifiableList(list);
    }

    public boolean isTernary() {
        return isTernary;
    }

    /**
     * @return the localized Name of this Permission as defined in the strings.xml for the current locale.
     * The identifier of the String constant is the lower case name of the enum constant with the prefix "perm_",
     * so for {@link #ADD_ODROID} it would be "perm_add_odroid".
     * If no localized name is found, the name of the constant as defined in the source code is used ({@link #name()}).
     */
    @NonNull
    public String toLocalizedString(Context context) {
        Resources res = context.getResources();
        int resId = res.getIdentifier("perm_" + this.name().toLowerCase(), "string", context.getPackageName());
        return resId == 0 ? name() : res.getString(resId);
    }

    /**
     * @return the localized description of this Permission as defined in the strings.xml for the current locale.
     * The identifier of the String constant is the lower case name of the enum constant with the prefix "perm-desc_",
     * so for {@link #ADD_ODROID} it would be "perm-desc_add_odroid".
     * If no localized description is found, the empty String is returned.
     */
    @NonNull
    public String getLocalizedDescription(Context context) {
        Resources res = context.getResources();
        int resId = res.getIdentifier("perm-desc_" + this.name().toLowerCase(), "string", context.getPackageName());
        return resId == 0 ? "" : res.getString(resId);
    }
}
