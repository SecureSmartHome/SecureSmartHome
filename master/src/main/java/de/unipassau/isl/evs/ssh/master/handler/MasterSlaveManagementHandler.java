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

import android.util.Log;

import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DeleteDevicePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.RegisterSlavePayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.database.AlreadyInUseException;
import de.unipassau.isl.evs.ssh.core.database.IsReferencedException;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.network.broadcast.ModuleBroadcaster;
import de.unipassau.isl.evs.ssh.master.network.broadcast.UserConfigurationBroadcaster;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_SLAVE_DELETE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_SLAVE_REGISTER;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.ADD_ODROID;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.DELETE_ODROID;

/**
 * Handles messages indicating that information of a device needs to be updated and writes these changes to the routing table.
 * <p/>
 * An example when this handler needs to take action is when a new sensor is added or switched to a new GPIO Pin.
 *
 * @author Leon Sell
 */
public class MasterSlaveManagementHandler extends AbstractMasterHandler implements Component {
    public static final Key<MasterSlaveManagementHandler> KEY = new Key<>(MasterSlaveManagementHandler.class);
    private static final String TAG = MasterSlaveManagementHandler.class.getSimpleName();
    private static final String MODULE_ADDED_JUST_BEFORE_SLAVE_DELETE_ERROR =
            "Error while deleting slave, because a module depends on it. This could be because a"
                    + " module was added just after deleting all module at this slave.";

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_SLAVE_REGISTER, MASTER_SLAVE_DELETE};
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_SLAVE_REGISTER.matches(message)) {
            handleSlaveRegister(message, MASTER_SLAVE_REGISTER.getPayload(message));
        } else if (MASTER_SLAVE_DELETE.matches(message)) {
            handleSlaveDelete(message, MASTER_SLAVE_DELETE.getPayload(message));
        } else {
            invalidMessage(message);
        }
    }

    private void handleSlaveDelete(Message.AddressedMessage message, DeleteDevicePayload deleteDevicePayload) {
        if (hasPermission(message.getFromID(), DELETE_ODROID)) {
            final SlaveController slaveController = requireComponent(SlaveController.KEY);
            List<Module> modulesAtSlave = slaveController.getModulesOfSlave(deleteDevicePayload.getUser());
            for (Module module : modulesAtSlave) {
                slaveController.removeModule(module.getName());
            }
            try {
                deleteSlave(deleteDevicePayload.getUser());
                requireComponent(UserConfigurationBroadcaster.KEY).updateAllClients();
            } catch (IsReferencedException e) {
                Log.i(TAG, MODULE_ADDED_JUST_BEFORE_SLAVE_DELETE_ERROR);
                sendReply(message, new Message(new ErrorPayload(e, MODULE_ADDED_JUST_BEFORE_SLAVE_DELETE_ERROR)));
            }
        } else {
            sendNoPermissionReply(message, DELETE_ODROID);
        }
    }

    private void handleSlaveRegister(Message.AddressedMessage message, RegisterSlavePayload registerSlavePayload) {
        if (hasPermission(message.getFromID(), ADD_ODROID)) {
            try {
                registerSlave(new Slave(
                        registerSlavePayload.getName(),
                        registerSlavePayload.getSlaveID(),
                        registerSlavePayload.getPassiveRegistrationToken()
                ));
                sendReply(message, new Message());
            } catch (AlreadyInUseException e) {
                Log.i(TAG, e.getLocalizedMessage());
                sendReply(message, new Message(new ErrorPayload(e)));
            }
        } else {
            sendNoPermissionReply(message, ADD_ODROID);
        }
    }

    public void registerSlave(Slave slave) throws AlreadyInUseException {
        requireComponent(SlaveController.KEY).addSlave(slave);
        final ModuleBroadcaster broadcaster = requireComponent(ModuleBroadcaster.KEY);
        broadcaster.updateAllClients();
    }

    public void deleteSlave(DeviceID slaveID) throws IsReferencedException {
        requireComponent(SlaveController.KEY).removeSlave(slaveID);
        final ModuleBroadcaster broadcaster = requireComponent(ModuleBroadcaster.KEY);
        broadcaster.updateAllClients();
    }
}