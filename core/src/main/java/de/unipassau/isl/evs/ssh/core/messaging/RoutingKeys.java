/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unipassau.isl.evs.ssh.core.messaging;

import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DeleteDevicePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DeviceConnectedPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBlockPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorStatusPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.GenerateNewRegisterTokenPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.GroupPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.HolidaySimulationPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModifyModulePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.RegisterSlavePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetGroupNamePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SetGroupTemplatePayload;
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
public enum RoutingKeys {
    ;

    private static final String PREFIX_MASTER = "/master";
    private static final String PREFIX_SLAVE = "/slave";
    private static final String PREFIX_APP = "/app";
    private static final String PREFIX_GLOBAL = "/global";

    // BEGIN: UserConfiguration
    public static final RoutingKey<SetPermissionPayload> MASTER_PERMISSION_SET = new RoutingKey<>(PREFIX_MASTER + "/permission/set", SetPermissionPayload.class);
    public static final RoutingKey<Void> MASTER_PERMISSION_SET_REPLY = MASTER_PERMISSION_SET.getReply(Void.class);
    public static final RoutingKey<ErrorPayload> MASTER_PERMISSION_SET_ERROR = MASTER_PERMISSION_SET.getReply(ErrorPayload.class);

    public static final RoutingKey<SetUserNamePayload> MASTER_USER_SET_NAME = new RoutingKey<>(PREFIX_MASTER + "/user/set/name", SetUserNamePayload.class);
    public static final RoutingKey<Void> MASTER_USERNAME_SET_REPLY = MASTER_USER_SET_NAME.getReply(Void.class);
    public static final RoutingKey<ErrorPayload> MASTER_USERNAME_SET_ERROR = MASTER_USER_SET_NAME.getReply(ErrorPayload.class);

    public static final RoutingKey<SetUserGroupPayload> MASTER_USER_SET_GROUP = new RoutingKey<>(PREFIX_MASTER + "/user/set/group", SetUserGroupPayload.class);
    public static final RoutingKey<Void> MASTER_USER_SET_GROUP_REPLY = MASTER_USER_SET_GROUP.getReply(Void.class);
    public static final RoutingKey<ErrorPayload> MASTER_USER_SET_GROUP_ERROR = MASTER_USER_SET_NAME.getReply(ErrorPayload.class);

    public static final RoutingKey<DeleteDevicePayload> MASTER_USER_DELETE = new RoutingKey<>(PREFIX_MASTER + "/user/delete", DeleteDevicePayload.class);
    public static final RoutingKey<Void> MASTER_USER_DELETE_REPLY = MASTER_USER_DELETE.getReply(Void.class);
    public static final RoutingKey<ErrorPayload> MASTER_USER_DELETE_ERROR = MASTER_USER_DELETE.getReply(ErrorPayload.class);

    public static final RoutingKey<GroupPayload> MASTER_GROUP_ADD = new RoutingKey<>(PREFIX_MASTER + "/group/add", GroupPayload.class);
    public static final RoutingKey<Void> MASTER_GROUP_ADD_REPLY = MASTER_GROUP_ADD.getReply(Void.class);
    public static final RoutingKey<ErrorPayload> MASTER_GROUP_ADD_ERROR = MASTER_GROUP_ADD.getReply(ErrorPayload.class);

    public static final RoutingKey<GroupPayload> MASTER_GROUP_DELETE = new RoutingKey<>(PREFIX_MASTER + "/group/delete", GroupPayload.class);
    public static final RoutingKey<Void> MASTER_GROUP_DELETE_REPLY = MASTER_GROUP_DELETE.getReply(Void.class);
    public static final RoutingKey<ErrorPayload> MASTER_GROUP_DELETE_ERROR = MASTER_GROUP_DELETE.getReply(ErrorPayload.class);

    public static final RoutingKey<SetGroupNamePayload> MASTER_GROUP_SET_NAME = new RoutingKey<>(PREFIX_MASTER + "/group/set/name", SetGroupNamePayload.class);
    public static final RoutingKey<Void> MASTER_GROUP_SET_NAME_REPLY = MASTER_GROUP_SET_NAME.getReply(Void.class);
    public static final RoutingKey<ErrorPayload> MASTER_GROUP_SET_NAME_ERROR = MASTER_GROUP_SET_NAME.getReply(ErrorPayload.class);

    public static final RoutingKey<SetGroupTemplatePayload> MASTER_GROUP_SET_TEMPLATE = new RoutingKey<>(PREFIX_MASTER + "/group/set/template", SetGroupTemplatePayload.class);
    public static final RoutingKey<Void> MASTER_GROUP_SET_TEMPLATE_REPLY = MASTER_GROUP_SET_TEMPLATE.getReply(Void.class);
    public static final RoutingKey<ErrorPayload> MASTER_GROUP_SET_TEMPLATE_ERROR = MASTER_GROUP_SET_TEMPLATE.getReply(ErrorPayload.class);

    public static final RoutingKey<GenerateNewRegisterTokenPayload> MASTER_USER_REGISTER = new RoutingKey<>(PREFIX_MASTER + "/user/register", GenerateNewRegisterTokenPayload.class);
    public static final RoutingKey<GenerateNewRegisterTokenPayload> MASTER_USER_REGISTER_REPLY = MASTER_USER_REGISTER.getReply(GenerateNewRegisterTokenPayload.class);
    public static final RoutingKey<ErrorPayload> MASTER_USER_REGISTER_ERROR = MASTER_USER_REGISTER.getReply(ErrorPayload.class);

    public static final RoutingKey<UserDeviceInformationPayload> APP_USERINFO_UPDATE = new RoutingKey<>(PREFIX_APP + "/userdevice/update", UserDeviceInformationPayload.class);
    // END: UserConfiguration

    // BEGIN: ModuleHandler
    public static final RoutingKey<ModifyModulePayload> MASTER_MODULE_ADD = new RoutingKey<>(PREFIX_MASTER + "/module/add", ModifyModulePayload.class);
    public static final RoutingKey<Void> MASTER_MODULE_ADD_REPLY = MASTER_MODULE_ADD.getReply(Void.class);
    public static final RoutingKey<ErrorPayload> MASTER_MODULE_ADD_ERROR = MASTER_MODULE_ADD.getReply(ErrorPayload.class);

    public static final RoutingKey<ModifyModulePayload> MASTER_MODULE_REMOVE = new RoutingKey<>(PREFIX_MASTER + "/module/remove", ModifyModulePayload.class);
    public static final RoutingKey<Void> MASTER_MODULE_REMOVE_REPLY = MASTER_MODULE_REMOVE.getReply(Void.class);
    public static final RoutingKey<ErrorPayload> MASTER_MODULE_REMOVE_ERROR = MASTER_MODULE_REMOVE.getReply(ErrorPayload.class);
    // END: ModuleHandler

    // BEGIN: SlaveManagementHandler
    public static final RoutingKey<RegisterSlavePayload> MASTER_SLAVE_REGISTER = new RoutingKey<>(PREFIX_MASTER + "/slave/register", RegisterSlavePayload.class);
    public static final RoutingKey<Void> MASTER_SLAVE_REGISTER_REPLY = MASTER_SLAVE_REGISTER.getReply(Void.class);
    public static final RoutingKey<ErrorPayload> MASTER_SLAVE_REGISTER_ERROR = MASTER_SLAVE_REGISTER.getReply(ErrorPayload.class);

    public static final RoutingKey<DeleteDevicePayload> MASTER_SLAVE_DELETE = new RoutingKey<>(PREFIX_MASTER + "/slave/delete", DeleteDevicePayload.class);
    public static final RoutingKey<Void> MASTER_SLAVE_DELETE_REPLY = MASTER_SLAVE_DELETE.getReply(Void.class);
    public static final RoutingKey<ErrorPayload> MASTER_SLAVE_DELETE_ERROR = MASTER_SLAVE_DELETE.getReply(ErrorPayload.class);
    // END: SlaveManagementHandler

    // BEGIN: Light
    public static final RoutingKey<LightPayload> MASTER_LIGHT_GET = new RoutingKey<>(PREFIX_MASTER + "/light/get", LightPayload.class);
    public static final RoutingKey<LightPayload> MASTER_LIGHT_GET_REPLY = MASTER_LIGHT_GET.getReply(LightPayload.class);
    public static final RoutingKey<ErrorPayload> MASTER_LIGHT_GET_ERROR = MASTER_LIGHT_GET.getReply(ErrorPayload.class);

    public static final RoutingKey<LightPayload> MASTER_LIGHT_SET = new RoutingKey<>(PREFIX_MASTER + "/light/set", LightPayload.class);
    public static final RoutingKey<LightPayload> MASTER_LIGHT_SET_REPLY = MASTER_LIGHT_SET.getReply(LightPayload.class);
    public static final RoutingKey<ErrorPayload> MASTER_LIGHT_SET_ERROR = MASTER_LIGHT_SET.getReply(ErrorPayload.class);

    public static final RoutingKey<LightPayload> SLAVE_LIGHT_GET = new RoutingKey<>(PREFIX_SLAVE + "/light/get", LightPayload.class);
    public static final RoutingKey<LightPayload> SLAVE_LIGHT_GET_REPLY = SLAVE_LIGHT_GET.getReply(LightPayload.class);
    public static final RoutingKey<ErrorPayload> SLAVE_LIGHT_GET_ERROR = SLAVE_LIGHT_GET.getReply(ErrorPayload.class);

    public static final RoutingKey<LightPayload> SLAVE_LIGHT_SET = new RoutingKey<>(PREFIX_SLAVE + "/light/set", LightPayload.class);
    public static final RoutingKey<LightPayload> SLAVE_LIGHT_SET_REPLY = SLAVE_LIGHT_SET.getReply(LightPayload.class);
    public static final RoutingKey<ErrorPayload> SLAVE_LIGHT_SET_ERROR = SLAVE_LIGHT_SET.getReply(ErrorPayload.class);

    public static final RoutingKey<LightPayload> APP_LIGHT_UPDATE = new RoutingKey<>(PREFIX_APP + "/light/update", LightPayload.class);
    // END: Light

    // BEGIN: Door
    public static final RoutingKey<DoorBellPayload> MASTER_DOOR_BELL_RING = new RoutingKey<>(PREFIX_MASTER + "/doorbell/ring", DoorBellPayload.class);

    public static final RoutingKey<DoorStatusPayload> MASTER_DOOR_STATUS_UPDATE = new RoutingKey<>(PREFIX_MASTER + "/door/update", DoorStatusPayload.class);

    public static final RoutingKey<DoorPayload> MASTER_DOOR_UNLATCH = new RoutingKey<>(PREFIX_MASTER + "/door/unlatch", DoorPayload.class);
    public static final RoutingKey<Void> MASTER_DOOR_UNLATCH_REPLY = MASTER_DOOR_UNLATCH.getReply(Void.class);
    public static final RoutingKey<ErrorPayload> MASTER_DOOR_UNLATCH_ERROR = MASTER_DOOR_UNLATCH.getReply(ErrorPayload.class);

    public static final RoutingKey<DoorBlockPayload> MASTER_DOOR_BLOCK = new RoutingKey<>(PREFIX_MASTER + "/door/block", DoorBlockPayload.class);
    public static final RoutingKey<DoorBlockPayload> MASTER_DOOR_BLOCK_REPLY = MASTER_DOOR_BLOCK.getReply(DoorBlockPayload.class);
    public static final RoutingKey<ErrorPayload> MASTER_DOOR_BLOCK_ERROR = MASTER_DOOR_BLOCK.getReply(ErrorPayload.class);

    public static final RoutingKey<DoorPayload> MASTER_DOOR_GET = new RoutingKey<>(PREFIX_MASTER + "/door/get", DoorPayload.class);
    public static final RoutingKey<DoorStatusPayload> MASTER_DOOR_GET_REPLY = MASTER_DOOR_GET.getReply(DoorStatusPayload.class);
    public static final RoutingKey<ErrorPayload> MASTER_DOOR_GET_ERROR = MASTER_DOOR_GET.getReply(ErrorPayload.class);

    public static final RoutingKey<DoorPayload> SLAVE_DOOR_UNLATCH = new RoutingKey<>(PREFIX_SLAVE + "/door/unlatch", DoorPayload.class);
    public static final RoutingKey<DoorPayload> SLAVE_DOOR_UNLATCH_REPLY = SLAVE_DOOR_UNLATCH.getReply(DoorPayload.class);
    public static final RoutingKey<ErrorPayload> SLAVE_DOOR_UNLATCH_ERROR = SLAVE_DOOR_UNLATCH.getReply(ErrorPayload.class);

    public static final RoutingKey<DoorStatusPayload> APP_DOOR_STATUS_UPDATE = new RoutingKey<>(PREFIX_APP + "/door/update", DoorStatusPayload.class);
    public static final RoutingKey<DoorBellPayload> APP_DOOR_RING = new RoutingKey<>(PREFIX_APP + "/door/ring", DoorBellPayload.class);
    // END: Door

    // BEGIN: Camera
    public static final RoutingKey<CameraPayload> MASTER_CAMERA_GET = new RoutingKey<>(PREFIX_MASTER + "/camera/get", CameraPayload.class);
    public static final RoutingKey<CameraPayload> MASTER_CAMERA_GET_REPLY = MASTER_CAMERA_GET.getReply(CameraPayload.class);
    public static final RoutingKey<ErrorPayload> MASTER_CAMERA_GET_ERROR = MASTER_CAMERA_GET.getReply(ErrorPayload.class);

    public static final RoutingKey<CameraPayload> SLAVE_CAMERA_GET = new RoutingKey<>(PREFIX_SLAVE + "/camera/get", CameraPayload.class);
    public static final RoutingKey<CameraPayload> SLAVE_CAMERA_GET_REPLY = SLAVE_CAMERA_GET.getReply(CameraPayload.class);
    public static final RoutingKey<ErrorPayload> SLAVE_CAMERA_GET_ERROR = SLAVE_CAMERA_GET.getReply(ErrorPayload.class);

    public static final RoutingKey<CameraPayload> APP_CAMERA_BROADCAST = new RoutingKey<>(PREFIX_APP + "camera/broadcast", CameraPayload.class);
    // END: Camera

    // BEGIN: Notification
    public static final RoutingKey<NotificationPayload> APP_NOTIFICATION_RECEIVE = new RoutingKey<>(PREFIX_APP + "/notification/receive", NotificationPayload.class);
    // END: Notification

    // BEGIN: HolidaySimulation
    public static final RoutingKey<HolidaySimulationPayload> MASTER_HOLIDAY_SET = new RoutingKey<>(PREFIX_MASTER + "/holiday/set", HolidaySimulationPayload.class);
    public static final RoutingKey<HolidaySimulationPayload> MASTER_HOLIDAY_SET_REPLY = MASTER_HOLIDAY_SET.getReply(HolidaySimulationPayload.class);
    public static final RoutingKey<ErrorPayload> MASTER_HOLIDAY_SET_ERROR = MASTER_HOLIDAY_SET.getReply(ErrorPayload.class);

    public static final RoutingKey<HolidaySimulationPayload> MASTER_HOLIDAY_GET = new RoutingKey<>(PREFIX_MASTER + "/holiday/get", HolidaySimulationPayload.class);
    public static final RoutingKey<HolidaySimulationPayload> MASTER_HOLIDAY_GET_REPLY = MASTER_HOLIDAY_GET.getReply(HolidaySimulationPayload.class);
    // END: HolidaySimulation

    // BEGIN: Weather
    public static final RoutingKey<ClimatePayload> MASTER_REQUEST_WEATHER_INFO = new RoutingKey<>(PREFIX_MASTER + "/weatherinfo/request", ClimatePayload.class);
    public static final RoutingKey<ClimatePayload> MASTER_REQUEST_WEATHER_INFO_REPLY = MASTER_REQUEST_WEATHER_INFO.getReply(ClimatePayload.class);
    public static final RoutingKey<ClimatePayload> MASTER_PUSH_WEATHER_INFO = new RoutingKey<>(PREFIX_MASTER + "/weatherinfo/push", ClimatePayload.class);
    // END: Weather

    public static final RoutingKey<SystemHealthPayload> MASTER_SYSTEM_HEALTH_CHECK = new RoutingKey<>(PREFIX_MASTER + "/systemhealth/check", SystemHealthPayload.class);

    public static final RoutingKey<DeviceConnectedPayload> MASTER_DEVICE_CONNECTED = new RoutingKey<>(PREFIX_MASTER + "/device/connected", DeviceConnectedPayload.class);

    public static final RoutingKey<ModulesPayload> GLOBAL_MODULES_UPDATE = new RoutingKey<>(PREFIX_GLOBAL + "/modules/update", ModulesPayload.class);
}
