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

import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

import static de.unipassau.isl.evs.ssh.core.messaging.Message.HEADER_REFERENCES_ID;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_CAMERA_BROADCAST;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_CAMERA_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_CAMERA_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_CAMERA_GET_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_CAMERA_GET_REPLY;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.BELL_RANG;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.REQUEST_CAMERA_STATUS;

/**
 * Handles messages requesting pictures from the camera and generates messages, containing the pictures,
 * and sends these to the responsible NotificationBroadcaster.
 *
 * @author Leon Sell
 */
public class MasterCameraHandler extends AbstractMasterHandler {

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                MASTER_CAMERA_GET,
                SLAVE_CAMERA_GET_REPLY,
                SLAVE_CAMERA_GET_ERROR
        };
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_CAMERA_GET.matches(message)) {
            handleGetRequest(message, MASTER_CAMERA_GET.getPayload(message));
        } else if (SLAVE_CAMERA_GET_REPLY.matches(message)) {
            handleResponse(message, SLAVE_CAMERA_GET_REPLY.getPayload(message));
        } else if (SLAVE_CAMERA_GET_ERROR.matches(message)) {
            handleError(message, SLAVE_CAMERA_GET_ERROR.getPayload(message));
        } else {
            invalidMessage(message);
        }
    }

    private void handleError(Message.AddressedMessage message, ErrorPayload payload) {
        Message reply = new Message(payload);
        Message.AddressedMessage originalMessage = takeProxiedReceivedMessage(message.getHeader(HEADER_REFERENCES_ID));
        sendReply(originalMessage, reply);
    }

    private void handleGetRequest(Message.AddressedMessage message, CameraPayload cameraPayload) {
        Message messageToSend = new Message(cameraPayload);

        //Check permission
        if (hasPermission(message.getFromID(), REQUEST_CAMERA_STATUS)) {
            Module atModule = requireComponent(SlaveController.KEY).getModule(cameraPayload.getModuleName());
            Message.AddressedMessage sentMessage = sendMessage(atModule.getAtSlave(), SLAVE_CAMERA_GET, messageToSend);
            recordReceivedMessageProxy(message, sentMessage);
        } else {
            //no permission
            sendNoPermissionReply(message, REQUEST_CAMERA_STATUS);
        }
    }

    private void handleResponse(Message.AddressedMessage message, CameraPayload cameraPayload) {
        Message reply = new Message(cameraPayload);
        Message.AddressedMessage originalMessage = takeProxiedReceivedMessage(message.getHeader(HEADER_REFERENCES_ID));
        if (!isMaster(originalMessage.getFromID())) {
            sendReply(originalMessage, reply);
        } else {
            //Broadcast picture too all devices that may also be informed that the bell rang
            sendMessageToAllDevicesWithPermission(reply, BELL_RANG, null, APP_CAMERA_BROADCAST);
        }
    }
}