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

package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.database.DatabaseControllerException;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.WrongAccessPointException;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModifyModulePayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.sec.Permission;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.network.broadcast.ModuleBroadcaster;
import de.unipassau.isl.evs.ssh.master.network.broadcast.UserConfigurationBroadcaster;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DEVICE_CONNECTED;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_ADD;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_REMOVE;

/**
 * The MasterModuleHandler sends updated lists of active Modules to ODROIDs and Clients
 *
 * @author Andreas Bucher
 * @author Wolfgang Popp
 */
public class MasterModuleHandler extends AbstractMasterHandler {

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_MODULE_ADD, MASTER_MODULE_REMOVE, MASTER_DEVICE_CONNECTED};
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_DEVICE_CONNECTED.matches(message)) {
            final DeviceID deviceID = MASTER_DEVICE_CONNECTED.getPayload(message).getDeviceID();
            final ModuleBroadcaster broadcaster = requireComponent(ModuleBroadcaster.KEY);
            broadcaster.updateClient(deviceID);
        } else if (MASTER_MODULE_ADD.matches(message)) {
            addModule(MASTER_MODULE_ADD.getPayload(message), message);
        } else if (MASTER_MODULE_REMOVE.matches(message)) {
            removeModule(MASTER_MODULE_REMOVE.getPayload(message), message);
        } else {
            invalidMessage(message);
        }
    }

    private void removeModule(ModifyModulePayload payload, Message.AddressedMessage original) {
        DeviceID fromID = original.getFromID();

        if (!hasPermission(fromID, Permission.DELETE_MODULE)) {
            sendNoPermissionReply(original, Permission.DELETE_MODULE);
            return;
        }
        requireComponent(SlaveController.KEY).removeModule(payload.getModule().getName());
        sendOnSuccess(original);
    }

    private void addModule(ModifyModulePayload payload, Message.AddressedMessage original) {
        DeviceID fromID = original.getFromID();

        if (!hasPermission(fromID, Permission.ADD_MODULE)) {
            sendNoPermissionReply(original, Permission.ADD_MODULE);
            return;
        }
        Module module = payload.getModule();

        if (!module.getModuleType().isValidAccessPoint(module.getModuleAccessPoint())) {
            sendError(original, new WrongAccessPointException(module.getModuleAccessPoint().getType()));
            return;
        }
        SlaveController slaveController = requireComponent(SlaveController.KEY);
        PermissionController permissionController = requireComponent(PermissionController.KEY);
        Permission[] permissions = Permission.getPermissions(module.getModuleType());
        try {
            slaveController.addModule(module);

            if (permissions != null) {
                for (Permission permission : permissions) {
                    permissionController.addPermission(permission, module.getName());
                }
            }
            sendOnSuccess(original);
        } catch (DatabaseControllerException e) {
            sendError(original, e);
        }
    }

    private void sendOnSuccess(Message.AddressedMessage original) {
        sendReply(original, new Message());

        final ModuleBroadcaster moduleBroadcaster = requireComponent(ModuleBroadcaster.KEY);
        moduleBroadcaster.updateAllClients();

        // also update the user configuration, since new permissions might have been added or removed
        final UserConfigurationBroadcaster userBroadcaster = requireComponent(UserConfigurationBroadcaster.KEY);
        userBroadcaster.updateAllClients();
    }

    private void sendError(Message.AddressedMessage original, Exception e) {
        sendReply(original, new Message(new ErrorPayload(e)));
    }
}
