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

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.network.broadcast.NotificationBroadcaster;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_CAMERA_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_BELL_RING;

/**
 * Handles messages received when the doorbell is used, requests a picture from the camera
 * (by also sending a message to the MasterCameraHandler) and generates messages for each target
 * and passes them to the OutgoingRouter.
 *
 * @author Leon Sell
 */
public class MasterDoorBellHandler extends AbstractMasterHandler {
    private static final String TAG = MasterDoorBellHandler.class.getSimpleName();

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_DOOR_BELL_RING};
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_DOOR_BELL_RING.matches(message)) {
            handleDoorBellRing(message, MASTER_DOOR_BELL_RING.getPayload(message));
        } else {
            invalidMessage(message);
        }
    }


    private void handleDoorBellRing(Message.AddressedMessage message, DoorBellPayload doorBellPayload) {
        //Check if message comes from a slave.
        if (isSlave(message.getFromID())) {
            //Notify clients
            final NotificationBroadcaster notificationBroadcaster = requireComponent(NotificationBroadcaster.KEY);
            notificationBroadcaster.sendMessageToAllReceivers(
                    NotificationPayload.NotificationType.BELL_RANG,
                    doorBellPayload
            );

            //Get picture
            //Camera has to be the first camera of all added cameras. (database and android camera id)
            SlaveController slaveController = requireComponent(SlaveController.KEY);
            final Module camera = slaveController.getModulesByType(CoreConstants.ModuleType.Webcam).get(0);
            final Message messageToSend = new Message(new CameraPayload(0, camera.getName()));

            final Message.AddressedMessage sentMessage = sendMessageLocal(MASTER_CAMERA_GET, messageToSend);
            recordReceivedMessageProxy(message, sentMessage);
        } else {
            Log.e(TAG, "A non slave device tried to send a slave only message.");
        }
    }
}