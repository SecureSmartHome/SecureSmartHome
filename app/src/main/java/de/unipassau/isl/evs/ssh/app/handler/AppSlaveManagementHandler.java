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

package de.unipassau.isl.evs.ssh.app.handler;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DeleteDevicePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.RegisterSlavePayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_SLAVE_DELETE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_SLAVE_DELETE_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_SLAVE_DELETE_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_SLAVE_REGISTER;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_SLAVE_REGISTER_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_SLAVE_REGISTER_REPLY;

/**
 * The AppSlaveManagementHandler handles the messaging needed to add and remove a slave from the system.
 *
 * @author Wolfgang Popp
 */
public class AppSlaveManagementHandler extends AbstractAppHandler implements Component {
    public static final Key<AppSlaveManagementHandler> KEY = new Key<>(AppSlaveManagementHandler.class);

    private final List<SlaveManagementListener> listeners = new LinkedList<>();

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                MASTER_SLAVE_REGISTER_REPLY,
                MASTER_SLAVE_REGISTER_ERROR,
                MASTER_SLAVE_DELETE_REPLY,
                MASTER_SLAVE_DELETE_ERROR
        };
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (!tryHandleResponse(message)) {
            if (MASTER_SLAVE_REGISTER_REPLY.matches(message)) {
                fireSlaveRegistered(true);
            } else if (MASTER_SLAVE_REGISTER_ERROR.matches(message)) {
                fireSlaveRegistered(false);
            } else if (MASTER_SLAVE_DELETE_REPLY.matches(message)) {
                fireSlaveRemoved(true);
            } else if (MASTER_SLAVE_DELETE_ERROR.matches(message)) {
                fireSlaveRemoved(false);
            } else {
                invalidMessage(message);
            }
        }
    }

    /**
     * Sends a message to master to registers a new slave.
     *
     * @param slaveID                  the device ID of the new slave
     * @param slaveName                the name of the new slave
     * @param passiveRegistrationToken the passive Registration token
     */
    public void registerNewSlave(DeviceID slaveID, String slaveName, byte[] passiveRegistrationToken) {
        RegisterSlavePayload payload = new RegisterSlavePayload(slaveName, slaveID, passiveRegistrationToken);
        final Future<Void> future = newResponseFuture(sendMessageToMaster(MASTER_SLAVE_REGISTER, new Message(payload)));
        future.addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                fireSlaveRegistered(future.isSuccess());
            }
        });
    }

    /**
     * Sends a message to master to delete the given slave.
     *
     * @param slaveID the slave to delete
     */
    public void deleteSlave(DeviceID slaveID) {
        DeleteDevicePayload payload = new DeleteDevicePayload(slaveID);
        final Future<Void> future = newResponseFuture(sendMessageToMaster(MASTER_SLAVE_DELETE, new Message(payload)));
        future.addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                fireSlaveRemoved(future.isSuccess());
            }
        });
    }

    /**
     * Adds the given SlaveManagementListener to this handler.
     *
     * @param listener the listener to add
     */
    public void addSlaveManagementListener(SlaveManagementListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the given SlaveManagementListener from this handler.
     *
     * @param listener the listener to remove
     */
    public void removeSlaveManagementListener(SlaveManagementListener listener) {
        listeners.remove(listener);
    }

    private void fireSlaveRegistered(boolean wasSuccessful) {
        for (SlaveManagementListener listener : listeners) {
            listener.onSlaveRegistered(wasSuccessful);
        }
    }

    private void fireSlaveRemoved(boolean wasSuccessful) {
        for (SlaveManagementListener listener : listeners) {
            listener.onSlaveRemoved(wasSuccessful);
        }
    }

    /**
     * The listener interface to get notified when a new slave has been registered or removed.
     */
    public interface SlaveManagementListener {
        /**
         * Called when the slave registration process finished.
         *
         * @param wasSuccessful true if the registration was successful, false otherwise
         */
        void onSlaveRegistered(boolean wasSuccessful);

        /**
         * Called when the slave remove process has finished.
         *
         * @param wasSuccessful true if the remove process was successful, false otherwise
         */
        void onSlaveRemoved(boolean wasSuccessful);
    }
}
