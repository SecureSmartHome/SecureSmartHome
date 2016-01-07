package de.unipassau.isl.evs.ssh.core.messaging;

import de.unipassau.isl.evs.ssh.core.messaging.payload.AddNewModulePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DeleteUserPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DeviceConnectedPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorLockPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorStatusPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorUnlatchPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.GenerateNewRegisterTokenPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.HolidaySimulationPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.RegisterSlavePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.RenameModulePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetPermissionPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetUserGroupPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetUserNamePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SystemHealthPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.UserDeviceInformationPayload;

/**
 * This class contains constants for RoutingKeys
 *
 * @author Team
 */
public class RoutingKeys {
    private RoutingKeys() {
    }

    private static final String PREFIX_MASTER = "/master";
    private static final String PREFIX_SLAVE = "/slave";
    private static final String PREFIX_APP = "/app";
    private static final String PREFIX_GLOBAL = "/global";

    // Master
    public static final RoutingKey<LightPayload> MASTER_LIGHT_GET = new RoutingKey<>(PREFIX_MASTER + "/light/get", LightPayload.class);
    public static final RoutingKey<LightPayload> MASTER_LIGHT_GET_REPLY = MASTER_LIGHT_GET.getReply(LightPayload.class);
    public static final RoutingKey<LightPayload> MASTER_LIGHT_SET_REPLY = MASTER_LIGHT_GET.getReply(LightPayload.class);
    public static final RoutingKey<ErrorPayload> MASTER_LIGHT_GET_ERROR = MASTER_LIGHT_GET.getReply(ErrorPayload.class);
    public static final RoutingKey<LightPayload> MASTER_LIGHT_SET = new RoutingKey<>(PREFIX_MASTER + "/light/set", LightPayload.class);

    public static final RoutingKey<DoorBellPayload> MASTER_DOOR_BELL_RING = new RoutingKey<>(PREFIX_MASTER + "/doorbell/ring", DoorBellPayload.class);
    @Deprecated //use MASTER_CAMERA_GET
    public static final RoutingKey<CameraPayload> MASTER_DOOR_BELL_CAMERA_GET = new RoutingKey<>(PREFIX_MASTER + "/doorbell/camera/get", CameraPayload.class);

    public static final RoutingKey<CameraPayload> MASTER_CAMERA_GET = new RoutingKey<>(PREFIX_MASTER + "/camera/get", CameraPayload.class);
    public static final RoutingKey<CameraPayload> MASTER_CAMERA_GET_REPLY = MASTER_CAMERA_GET.getReply(CameraPayload.class);

    public static final RoutingKey<MessagePayload> MASTER_REQUEST_WEATHER_INFO = new RoutingKey<>(PREFIX_MASTER + "/weatherinfo/request", MessagePayload.class);
    public static final RoutingKey<ClimatePayload> MASTER_PUSH_WEATHER_INFO = new RoutingKey<>(PREFIX_MASTER + "/weatherinfo/push", ClimatePayload.class);

    public static final RoutingKey<NotificationPayload> MASTER_NOTIFICATION_SEND = new RoutingKey<>(PREFIX_MASTER + "/notification/send", NotificationPayload.class);

    public static final RoutingKey<DoorStatusPayload> MASTER_DOOR_STATUS_GET = new RoutingKey<>(PREFIX_MASTER + "/door/status_get", DoorStatusPayload.class);
    public static final RoutingKey<DoorLockPayload> MASTER_DOOR_LOCK_SET = new RoutingKey<>(PREFIX_MASTER + "/door/lock_set", DoorLockPayload.class);
    public static final RoutingKey<DoorLockPayload> MASTER_DOOR_LOCK_GET = new RoutingKey<>(PREFIX_MASTER + "/door/lock_get", DoorLockPayload.class);
    public static final RoutingKey<DoorUnlatchPayload> MASTER_DOOR_UNLATCH = new RoutingKey<>(PREFIX_MASTER + "/door/unlatch", DoorUnlatchPayload.class);

    public static final RoutingKey<GenerateNewRegisterTokenPayload> MASTER_USER_REGISTER = new RoutingKey<>(PREFIX_MASTER + "/user/register", GenerateNewRegisterTokenPayload.class);

    // BEGIN: MasterUserConfigHandler
    public static final RoutingKey<SetPermissionPayload> MASTER_PERMISSION_SET = new RoutingKey<>(PREFIX_MASTER + "/permission/set", SetPermissionPayload.class);
    public static final RoutingKey<Void> MASTER_PERMISSION_SET_REPLY = MASTER_PERMISSION_SET.getReply(Void.class);
    public static final RoutingKey<ErrorPayload> MASTER_PERMISSION_SET_ERROR = MASTER_PERMISSION_SET.getError(ErrorPayload.class);

    public static final RoutingKey<SetUserNamePayload> MASTER_USERNAME_SET = new RoutingKey<>(PREFIX_MASTER + "/user/name/set", SetUserNamePayload.class);
    public static final RoutingKey<Void> MASTER_USERNAME_SET_REPLY = MASTER_USERNAME_SET.getReply(Void.class);
    public static final RoutingKey<ErrorPayload> MASTER_USERNAME_SET_ERROR = MASTER_USERNAME_SET.getError(ErrorPayload.class);

    public static final RoutingKey<SetUserGroupPayload> MASTER_USERGROUP_SET = new RoutingKey<>(PREFIX_MASTER + "/user/group/set", SetUserGroupPayload.class);
    public static final RoutingKey<Void> MASTER_USERGROUP_SET_REPLY = MASTER_USERGROUP_SET.getReply(Void.class);
    public static final RoutingKey<ErrorPayload> MASTER_USERGROUP_SET_ERROR = MASTER_USERNAME_SET.getError(ErrorPayload.class);

    public static final RoutingKey<DeleteUserPayload> MASTER_USER_DELETE = new RoutingKey<>(PREFIX_MASTER + "/user/delete", DeleteUserPayload.class);
    public static final RoutingKey<Void> MASTER_USER_DELETE_REPLY = MASTER_USER_DELETE.getReply(Void.class);
    public static final RoutingKey<ErrorPayload> MASTER_USER_DELETE_ERROR = MASTER_USER_DELETE.getError(ErrorPayload.class);
    // END: MasterUserConfigHandler

    public static final RoutingKey<HolidaySimulationPayload> MASTER_HOLIDAY_SET = new RoutingKey<>(PREFIX_MASTER + "/holiday/set", HolidaySimulationPayload.class);
    public static final RoutingKey<HolidaySimulationPayload> MASTER_HOLIDAY_GET = new RoutingKey<>(PREFIX_MASTER + "/holiday/get", HolidaySimulationPayload.class);

    public static final RoutingKey<AddNewModulePayload> MASTER_MODULE_ADD = new RoutingKey<>(PREFIX_MASTER + "/module/add", AddNewModulePayload.class);
    public static final RoutingKey<MessagePayload> MASTER_MODULE_GET = new RoutingKey<>(PREFIX_MASTER + "/module/get", MessagePayload.class);
    public static final RoutingKey<RenameModulePayload> MASTER_MODULE_RENAME = new RoutingKey<>(PREFIX_MASTER + "/module/modify", RenameModulePayload.class);

    public static final RoutingKey<RegisterSlavePayload> MASTER_SLAVE_REGISTER = new RoutingKey<>(PREFIX_MASTER + "/slave/register", RegisterSlavePayload.class);
    public static final RoutingKey<SystemHealthPayload> MASTER_SYSTEM_HEALTH_CHECK = new RoutingKey<>(PREFIX_MASTER + "/systemhealth/check", SystemHealthPayload.class);

    public static final RoutingKey<DeviceConnectedPayload> MASTER_DEVICE_CONNECTED = new RoutingKey<>(PREFIX_MASTER + "/device/connected", DeviceConnectedPayload.class);

    // Slave
    public static final RoutingKey<MessagePayload> SLAVE_LIGHT_GET = new RoutingKey<>(PREFIX_SLAVE + "/light/get", MessagePayload.class);
    public static final RoutingKey<LightPayload> SLAVE_LIGHT_SET = new RoutingKey<>(PREFIX_SLAVE + "/light/set", LightPayload.class);
    public static final RoutingKey<LightPayload> SLAVE_LIGHT_GET_REPLY = SLAVE_LIGHT_GET.getReply(LightPayload.class);
    public static final RoutingKey<LightPayload> SLAVE_LIGHT_SET_REPLY = SLAVE_LIGHT_GET.getReply(LightPayload.class);
    public static final RoutingKey<CameraPayload> SLAVE_CAMERA_GET = new RoutingKey<>(PREFIX_SLAVE + "/camera/get", CameraPayload.class);
    public static final RoutingKey<DoorStatusPayload> SLAVE_DOOR_STATUS_GET = new RoutingKey<>(PREFIX_SLAVE + "/door/status_get", DoorStatusPayload.class);
    public static final RoutingKey<DoorUnlatchPayload> SLAVE_DOOR_UNLATCH = new RoutingKey<>(PREFIX_SLAVE + "/door/unlatch", DoorUnlatchPayload.class);

    // App
    public static final RoutingKey<MessagePayload> APP_MODULES_GET = new RoutingKey<>(PREFIX_APP + "/module/get", MessagePayload.class);
    public static final RoutingKey<LightPayload> APP_LIGHT_UPDATE = new RoutingKey<>(PREFIX_APP + "/light/update", LightPayload.class);
    public static final RoutingKey<ClimatePayload> APP_CLIMATE_UPDATE = new RoutingKey<>(PREFIX_APP + "/climate/update", ClimatePayload.class);
    public static final RoutingKey<NotificationPayload> APP_NOTIFICATION_RECEIVE = new RoutingKey<>(PREFIX_APP + "/notification/receive", NotificationPayload.class);
    public static final RoutingKey<CameraPayload> APP_CAMERA_GET = new RoutingKey<>(PREFIX_APP + "/camera/get", CameraPayload.class);
    public static final RoutingKey<DoorLockPayload> APP_DOOR_BLOCK = new RoutingKey<>(PREFIX_APP + "/door/block", DoorLockPayload.class);
    public static final RoutingKey<DoorStatusPayload> APP_DOOR_GET = new RoutingKey<>(PREFIX_APP + "/door/get", DoorStatusPayload.class);
    public static final RoutingKey<DoorBellPayload> APP_DOOR_RING = new RoutingKey<>(PREFIX_APP + "/door/ring", DoorBellPayload.class);
    public static final RoutingKey<UserDeviceInformationPayload> APP_USERINFO_UPDATE = new RoutingKey<>(PREFIX_APP + "/userdevice/update", UserDeviceInformationPayload.class);
    public static final RoutingKey<AddNewModulePayload> APP_MODULE_ADD = new RoutingKey<>(PREFIX_APP + "/module/add", AddNewModulePayload.class);
    public static final RoutingKey<GenerateNewRegisterTokenPayload> APP_USER_REGISTER = new RoutingKey<>(PREFIX_APP + "/user/register", GenerateNewRegisterTokenPayload.class);
    public static final RoutingKey<MessagePayload> APP_SLAVE_REGISTER = new RoutingKey<>(PREFIX_APP + "/slave/register", MessagePayload.class);
    public static final RoutingKey<HolidaySimulationPayload> APP_HOLIDAY_GET = new RoutingKey<>(PREFIX_APP + "/holiday/get", HolidaySimulationPayload.class);

    // Global
    public static final RoutingKey<ModulesPayload> GLOBAL_MODULES_UPDATE = new RoutingKey<>(PREFIX_GLOBAL + "/modules/update", ModulesPayload.class);
}
