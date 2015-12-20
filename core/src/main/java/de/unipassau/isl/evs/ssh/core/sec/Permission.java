package de.unipassau.isl.evs.ssh.core.sec;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import java.util.Arrays;

import de.unipassau.isl.evs.ssh.core.CoreConstants;

/**
 * //TODO why is the ID of the constant not also used as name for the database, so that {@link #valueOf(String)} could be used (Niko, 2015-12-20)
 *
 * @author Wolfgang Popp
 * @author Leon Sell
 */
public enum Permission {
    //Odroid
    ADD_ODROID("AddOdroid"),
    RENAME_ODROID("RenameOdroid"),
    DELETE_ODROID("DeleteOdroid"),

    //Sensor
    ADD_SENSOR("AddSensor"),
    RENAME_MODULE("RenameSensor"),
    DELETE_SENSOR("DeleteSensor"),

    //Light
    REQUEST_LIGHT_STATUS("RequestLightStatus"),

    // Window
    REQUEST_WINDOW_STATUS("RequestWindowStatus"),

    //Door
    REQUEST_DOOR_STATUS("RequestDoorStatus"),
    LOCK_DOOR("LockDoor"),
    UNLATCH_DOOR("UnlatchDoor"),

    //Camera
    REQUEST_CAMERA_STATUS("RequestCameraStatus"),
    TAKE_CAMERA_PICTURE("TakeCameraPicture"),

    //WeaterStation
    REQUEST_WEATHER_STATUS("RequestWeatherStatus"),

    //HolidaySimulation
    START_HOLIDAY_SIMULATION("StartHolidaySimulation"),
    STOP_HOLIDAY_SIMULATION("StopHolidaySimulation"),

    //User
    ADD_USER("AddUser"),
    DELETE_USER("DeleteUser"),
    CHANGE_USER_NAME("ChangeUserName"),
    CHANGE_USER_GROUP("ChangeUserGroup"),
    GRANT_USER_PERMISSION("GrantUserPermission"),
    WITHDRAW_USER_PERMISSION("WithdrawUserPermission"),

    //Groups
    ADD_GROUP("AddGroup"),
    DELETE_GROUP("DeleteGroup"),
    CHANGE_GROUP_NAME("ChangeGroupName"),
    SHOW_GROUP_MEMBER("ShowGroupMembers"),
    CHANGE_GROUP_TEMPLATE("ChangeGroupTemplate"),

    //Templates
    CREATE_TEMPLATE("CreateTemplate"),
    DELETE_TEMPLATE("DeleteTemplate"),
    EDIT_TEMPLATE("EditTemplate"),
    SHOW_TEMPLATE_PERMISSION("ShowTemplatePermission"),

    //Notification Types
    ODROID_ADDED("OdroidAdded"),
    HUMIDITY_WARNING("HumidityWarning"),
    BRIGHTNESS_WARNING("BrightnessWarning"),
    HOLIDAY_MODE_SWITCHED_ON("HolidayModeSwitchedOn"),
    HOLIDAY_MODE_SWITCHED_OFF("HolidayModeSwitchedOff"),
    SYSTEM_HEALTH_WARNING("SystemHealthWarning"),
    BELL_RANG("BellRang"),
    WEATHER_WARNING("WeatherWarning"),
    DOOR_UNLATCHED("DoorOpened"),
    DOOR_LOCKED("DoorLocked"),
    DOOR_UNLOCKED("DoorUnlocked"),

    //Ternary Permissions
    SWITCH_LIGHT("SwitchLight", true);

    private final String name;
    private final boolean isTernary;

    Permission(String name) {
        this.name = name;
        this.isTernary = false;
    }

    Permission(String name, boolean isTernary) {
        this.name = name;
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

    public static final Permission[] binaryPermissions = Iterables.toArray(
            Iterables.filter(Arrays.asList(values()),
                    new Predicate<Permission>() {
                        @Override
                        public boolean apply(Permission input) {
                            return !input.isTernary();
                        }
                    }
            ),
            Permission.class
    );

    @Override
    public String toString() {
        return name;
    }
}
