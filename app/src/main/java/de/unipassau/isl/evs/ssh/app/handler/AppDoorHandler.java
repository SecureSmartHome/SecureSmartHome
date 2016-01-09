package de.unipassau.isl.evs.ssh.app.handler;

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
import de.unipassau.isl.evs.ssh.core.network.Client;
import io.netty.util.concurrent.FailedFuture;
import io.netty.util.concurrent.Future;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_DOOR_RING;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_DOOR_STATUS_UPDATE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_CAMERA_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_CAMERA_GET_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_BLOCK;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_BLOCK_REPLY;
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
    private List<DoorListener> listeners = new LinkedList<>();
    private byte[] picture = null;

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

    private void fireImageUpdated(byte[] image) {
        if (image == null) {
            Log.v(TAG, "No camera picture came with the message.");
        } else {
            Log.v(TAG, "Received picture.");
        }
        for (DoorListener listener : listeners) {
            listener.onPictureChanged(image);
        }
    }

    private void fireStatusUpdated() {
        for (DoorListener listener : listeners) {
            listener.onDoorStatusChanged();
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                APP_DOOR_RING,
                APP_DOOR_STATUS_UPDATE,
                MASTER_DOOR_BLOCK_REPLY,
                MASTER_DOOR_UNLATCH_REPLY,
                MASTER_DOOR_UNLATCH_ERROR,
                MASTER_CAMERA_GET_REPLY
        };
    }

    @Override
    public void handle(Message.AddressedMessage message) {

        if (MASTER_DOOR_BLOCK_REPLY.matches(message)) {
            isDoorBlocked = MASTER_DOOR_BLOCK_REPLY.getPayload(message).isLock();
            handleResponse(message);
        } else if (MASTER_DOOR_UNLATCH_REPLY.matches(message)) {
            handleResponse(message);
        } else if (MASTER_DOOR_UNLATCH_ERROR.matches(message)) {
            handleResponse(message);
        } else if (APP_DOOR_STATUS_UPDATE.matches(message)) {
            DoorStatusPayload payload = APP_DOOR_STATUS_UPDATE.getPayload(message);
            isDoorOpen = payload.isOpen();
            isDoorBlocked = payload.isBlocked();
            fireStatusUpdated();
        } else if (MASTER_CAMERA_GET_REPLY.matches(message)) {
            picture = MASTER_CAMERA_GET_REPLY.getPayload(message).getPicture();
            handleResponse(message);
        } else if (APP_DOOR_RING.matches(message)) {
            DoorBellPayload doorBellPayload = APP_DOOR_RING.getPayload(message);
            fireImageUpdated(doorBellPayload.getCameraPayload().getPicture());
        } else {
            invalidMessage(message);
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
    public void openDoor() {
        List<Module> doors = requireComponent(AppModuleHandler.KEY).getDoors();
        if (doors.size() < 1) {
            Log.e(TAG, "Could not open the door. No door installed");
            return;
        }
        DoorPayload doorPayload = new DoorPayload(doors.get(0).getName());
        sendMessageToMaster(MASTER_DOOR_UNLATCH, new Message(doorPayload));
        isDoorOpen = true;
    }

    private void blockDoor(boolean isBlocked) {
        List<Module> doors = requireComponent(AppModuleHandler.KEY).getDoors();
        if (doors.size() < 1) {
            Log.e(TAG, "Could not (un)block the door. No door installed");
            return;
        }
        DoorBlockPayload doorPayload = new DoorBlockPayload(isBlocked, doors.get(0).getName());
        sendMessageToMaster(MASTER_DOOR_BLOCK, new Message(doorPayload));
        isDoorBlocked = isBlocked;
    }

    /**
     * Gets the last taken picture.
     *
     * @return the last taken picture or null if no picture has been taken yet.
     */
    public byte[] getPicture() {
        return picture;
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
        blockDoor(true);
    }

    /**
     * Refreshes the door photo by sending a message.
     */
    public Future<CameraPayload> refreshImage() {
        List<Module> cameras = requireComponent(AppModuleHandler.KEY).getCameras();
        if (cameras.size() < 1) {
            Log.e(TAG, "Could not refresh the door image. No camera available");
            //TODO Niko: move to method "newFailedFuture" (Wolfi, 2016-01-09)
            return new FailedFuture<>(requireComponent(Client.KEY).getAliveExecutor().next(),
                    new IllegalStateException("Could not refresh the door image. No camera available"));
        }
        CameraPayload payload = new CameraPayload(0, cameras.get(0).getName());
        return newResponseFuture(
                sendMessageToMaster(MASTER_CAMERA_GET, new Message(payload))
        );
    }

    /**
     * The listener interface to receive door events.
     */
    public interface DoorListener {
        void onPictureChanged(byte[] image);

        void onDoorStatusChanged();
    }
}
