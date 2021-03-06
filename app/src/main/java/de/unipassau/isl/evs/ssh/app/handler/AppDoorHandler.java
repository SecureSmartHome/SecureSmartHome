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

import android.support.annotation.Nullable;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBlockPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorStatusPayload;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_CAMERA_BROADCAST;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_DOOR_RING;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_DOOR_STATUS_UPDATE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_CAMERA_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_CAMERA_GET_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_CAMERA_GET_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_BLOCK;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_BLOCK_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_BLOCK_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_GET_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_GET_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_UNLATCH;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_UNLATCH_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_UNLATCH_REPLY;

/**
 * The AppDoorHandler class sends and receives messages for door events.
 *
 * @author Wolfgang Popp
 */
public class AppDoorHandler extends AbstractAppHandler implements Component {
    public static final Key<AppDoorHandler> KEY = new Key<>(AppDoorHandler.class);
    private static final String TAG = AppDoorHandler.class.getSimpleName();

    private boolean isDoorBlocked = false;
    private boolean isDoorOpen = false;
    private final List<DoorListener> listeners = new LinkedList<>();
    private byte[] picture = null;

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                APP_DOOR_RING,
                APP_DOOR_STATUS_UPDATE,
                APP_CAMERA_BROADCAST,
                MASTER_DOOR_GET_REPLY,
                MASTER_DOOR_GET_ERROR,
                MASTER_DOOR_BLOCK_REPLY,
                MASTER_DOOR_BLOCK_ERROR,
                MASTER_DOOR_UNLATCH_REPLY,
                MASTER_DOOR_UNLATCH_ERROR,
                MASTER_CAMERA_GET_REPLY,
                MASTER_CAMERA_GET_ERROR,
        };
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (!tryHandleResponse(message)) {
            if (MASTER_DOOR_GET_REPLY.matches(message)) {
                handleUpdate(MASTER_DOOR_GET_REPLY.getPayload(message));
            } else if (MASTER_DOOR_GET_ERROR.matches(message)) {
                fireStatusUpdated(false);
            } else if (MASTER_DOOR_BLOCK_REPLY.matches(message)) {
                isDoorBlocked = MASTER_DOOR_BLOCK_REPLY.getPayload(message).isBlock();
                fireBlockActionFinished(true);
            } else if (MASTER_DOOR_BLOCK_ERROR.matches(message)) {
                fireBlockActionFinished(false);
            } else if (MASTER_DOOR_UNLATCH_REPLY.matches(message)) {
                fireUnlatchActionFinished(true);
            } else if (MASTER_DOOR_UNLATCH_ERROR.matches(message)) {
                fireUnlatchActionFinished(false);
            } else if (MASTER_CAMERA_GET_REPLY.matches(message)) {
                picture = MASTER_CAMERA_GET_REPLY.getPayload(message).getPicture();
                fireCameraActionFinished(true);
            } else if (APP_CAMERA_BROADCAST.matches(message)) {
                picture = APP_CAMERA_BROADCAST.getPayload(message).getPicture();
                fireCameraActionFinished(true);
            } else if (MASTER_CAMERA_GET_ERROR.matches(message)) {
                fireCameraActionFinished(false);
            } else if (APP_DOOR_STATUS_UPDATE.matches(message)) {
                handleUpdate(APP_DOOR_STATUS_UPDATE.getPayload(message));
            } else if (APP_DOOR_RING.matches(message)) {
                DoorBellPayload doorBellPayload = APP_DOOR_RING.getPayload(message);
                picture = doorBellPayload.getCameraPayload().getPicture();
                fireCameraActionFinished(picture != null);
            } else {
                invalidMessage(message);
            }
        }
    }

    private void handleUpdate(DoorStatusPayload payload) {
        isDoorOpen = payload.isOpen();
        isDoorBlocked = payload.isBlocked();
        fireStatusUpdated(true);
    }

    @Nullable
    private String getDoor() {
        List<Module> doors = requireComponent(AppModuleHandler.KEY).getDoorBuzzers();
        if (doors.size() < 1) {
            return null;
        }
        return doors.get(0).getName();
    }

    /**
     * Adds a DoorListener to this handler.
     *
     * @param listener the DoorListener that is added to handler.
     */
    public void addListener(DoorListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the given DoorListener from this handler.
     *
     * @param listener the DoorListener that is removed from handler.
     */
    public void removeListener(DoorListener listener) {
        listeners.remove(listener);
    }

    private void fireStatusUpdated(boolean wasSuccessful) {
        for (DoorListener listener : listeners) {
            listener.onDoorStatusChanged(wasSuccessful);
        }
    }

    private void fireBlockActionFinished(boolean wasSuccessful) {
        for (DoorListener listener : listeners) {
            listener.blockActionFinished(wasSuccessful);
        }
    }

    private void fireUnlatchActionFinished(boolean wasSuccessful) {
        for (DoorListener listener : listeners) {
            listener.unlatchActionFinished(wasSuccessful);
        }
    }

    private void fireCameraActionFinished(boolean wasSuccessful) {
        for (DoorListener listener : listeners) {
            listener.cameraActionFinished(wasSuccessful);
        }

    }

    /**
     * Checks if the door is open.
     *
     * @return true if the door is open
     */
    public boolean isOpen() {
        return isDoorOpen;
    }

    /**
     * Checks if the door is blocked.
     *
     * @return true if the door is blocked
     */
    public boolean isBlocked() {
        return isDoorBlocked;
    }

    /**
     * Sends a "OpenDoor" message to the master.
     */
    public void unlatchDoor() {
        final String door = getDoor();
        if (door == null) {
            Log.e(TAG, "Could not open the door. No door buzzer installed");
            fireUnlatchActionFinished(false);
            return;
        }

        DoorPayload doorPayload = new DoorPayload(door);
        final Message.AddressedMessage messageToMaster = sendMessageToMaster(MASTER_DOOR_UNLATCH, new Message(doorPayload));
        final Future<Void> messagePayloadFuture = newResponseFuture(messageToMaster);
        messagePayloadFuture.addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                fireUnlatchActionFinished(future.isSuccess());
            }
        });
    }

    /**
     * Sends a "BlockDoor" message to the master.
     */
    public void blockDoor() {
        blockDoor(true);
    }

    /**
     * Sends a "UnblockDoor" message to the master.
     */
    public void unblockDoor() {
        blockDoor(false);
    }

    private void blockDoor(boolean isBlocked) {
        final String door = getDoor();
        if (door == null) {
            Log.e(TAG, "Could not (un)block the door. No door sensor installed");
            fireBlockActionFinished(false);
            return;
        }

        DoorBlockPayload doorPayload = new DoorBlockPayload(isBlocked, door);
        final Future<DoorStatusPayload> future = newResponseFuture(sendMessageToMaster(MASTER_DOOR_BLOCK, new Message(doorPayload)));
        future.addListener(new FutureListener<DoorStatusPayload>() {
            @Override
            public void operationComplete(Future<DoorStatusPayload> future) throws Exception {
                boolean wasSuccessful = future.isSuccess();
                if (wasSuccessful) {
                    final DoorStatusPayload payload = future.get();
                    isDoorBlocked = payload.isBlocked();
                    isDoorOpen = payload.isOpen();
                }
                fireBlockActionFinished(wasSuccessful);
            }
        });
    }

    /**
     * Refreshes the door photo by sending a message.
     */
    public void refreshImage() {
        List<Module> cameras = requireComponent(AppModuleHandler.KEY).getCameras();
        if (cameras.size() < 1) {
            Log.e(TAG, "Could not refresh the door image. No camera available");
            fireCameraActionFinished(false);
            return;
        }
        CameraPayload payload = new CameraPayload(0, cameras.get(0).getName());
        final Future<CameraPayload> future = newResponseFuture(sendMessageToMaster(MASTER_CAMERA_GET, new Message(payload)));
        future.addListener(new FutureListener<CameraPayload>() {
            @Override
            public void operationComplete(Future<CameraPayload> future) throws Exception {
                final boolean success = future.isSuccess();
                if (success) {
                    picture = future.get().getPicture();
                }
                fireCameraActionFinished(success);
            }
        });
    }

    /**
     * Gets the last taken picture.
     *
     * @return the last taken picture or null if no picture has been taken yet.
     */
    @Nullable
    public byte[] getPicture() {
        return picture;
    }

    /**
     * Requests the door status from master by sending him a message.
     */
    public void requestDoorStatus() {
        Message message = new Message(new DoorPayload(getDoor()));
        final Future<DoorStatusPayload> future = newResponseFuture(sendMessageToMaster(MASTER_DOOR_GET, message));
        future.addListener(new FutureListener<DoorStatusPayload>() {
            @Override
            public void operationComplete(Future<DoorStatusPayload> future) throws Exception {
                if (future.isSuccess()) {
                    handleUpdate(future.get());
                }
            }
        });
    }

    /**
     * The listener interface to receive door events.
     */
    public interface DoorListener {

        /**
         * Called when the door status changed. This happens either when the door was (un)blocked, unlatched or opened.
         *
         * @param wasSuccessful true when the action finished successfully
         */
        void onDoorStatusChanged(boolean wasSuccessful);

        /**
         * Called when a door (un)block action finished.
         *
         * @param wasSuccessful true when the action finished successfully
         */
        void blockActionFinished(boolean wasSuccessful);

        /**
         * Called when a door unlatch action finished.
         *
         * @param wasSuccessful true when the action finished successfully
         */
        void unlatchActionFinished(boolean wasSuccessful);

        /**
         * Called when a camera request finished.
         *
         * @param wasSuccessful true when the action finished successfully
         */
        void cameraActionFinished(boolean wasSuccessful);
    }
}
