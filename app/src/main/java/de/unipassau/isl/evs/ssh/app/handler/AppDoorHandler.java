package de.unipassau.isl.evs.ssh.app.handler;

import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorLockPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorStatusPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorUnlatchPayload;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_CAMERA_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_DOOR_BLOCK;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_DOOR_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_DOOR_RING;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_NOTIFICATION_RECEIVE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_CAMERA_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_LOCK_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_LOCK_SET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_STATUS_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_UNLATCH;

/**
 * The AppDoorHandler class sends and receives messages for door events.
 *
 * @author Wolfgang Popp
 */
public class AppDoorHandler extends AbstractMessageHandler implements Component {
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
        return new RoutingKey[]{APP_CAMERA_GET,
                APP_DOOR_BLOCK,
                APP_DOOR_GET,
                APP_DOOR_RING};
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (APP_CAMERA_GET.matches(message)) {
            CameraPayload payload = APP_CAMERA_GET.getPayload(message);
            picture = payload.getPicture();
            fireImageUpdated(picture);
        } else if (APP_DOOR_BLOCK.matches(message)) {
            DoorLockPayload payload = APP_DOOR_BLOCK.getPayload(message);
            isDoorBlocked = !payload.isUnlock();
            fireStatusUpdated();
        } else if (APP_DOOR_GET.matches(message)) {
            DoorStatusPayload payload = APP_DOOR_GET.getPayload(message);
            isDoorOpen = !payload.isClosed();
            fireStatusUpdated();
        } else if (APP_DOOR_RING.matches(message)) {
            DoorBellPayload doorBellPayload = APP_DOOR_RING.getPayload(message);
            fireImageUpdated(doorBellPayload.getCameraPayload().getPicture());
            Message messageToSend = new Message(doorBellPayload);
            requireComponent(OutgoingRouter.KEY).sendMessageLocal(APP_NOTIFICATION_RECEIVE,
                    messageToSend);
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
     * Refreshs the door status by sending a status request message.
     */
    public void refreshDoorStatus() {
        List<Module> doors = requireComponent(AppModuleHandler.KEY).getDoors();
        if (doors.size() < 1) {
            Log.e(TAG, "Could not get door status. No door installed");
            return;
        }
        refreshBlockStatus(doors.get(0).getName());
        refreshOpenStatus(doors.get(0).getName());
    }

    private void refreshOpenStatus(String door) {
        DoorStatusPayload doorPayload = new DoorStatusPayload(false, door);

        Message message = new Message(doorPayload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, APP_DOOR_GET.getKey());

        OutgoingRouter router = requireComponent(OutgoingRouter.KEY);
        router.sendMessageToMaster(MASTER_DOOR_STATUS_GET, message);

    }

    private void refreshBlockStatus(String door) {
        DoorUnlatchPayload doorPayload = new DoorUnlatchPayload(door);

        Message message = new Message(doorPayload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, APP_DOOR_BLOCK.getKey());

        OutgoingRouter router = requireComponent(OutgoingRouter.KEY);
        router.sendMessageToMaster(MASTER_DOOR_LOCK_GET, message);
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
        DoorUnlatchPayload doorPayload = new DoorUnlatchPayload(doors.get(0).getName());

        Message message = new Message(doorPayload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, APP_DOOR_GET.getKey());

        OutgoingRouter router = requireComponent(OutgoingRouter.KEY);
        router.sendMessageToMaster(MASTER_DOOR_UNLATCH, message);
        isDoorOpen = true;
    }

    private void blockDoor(boolean isBlocked) {
        List<Module> doors = requireComponent(AppModuleHandler.KEY).getDoors();
        if (doors.size() < 1) {
            Log.e(TAG, "Could not (un)block the door. No door installed");
            return;
        }
        DoorLockPayload doorPayload = new DoorLockPayload(isBlocked, doors.get(0).getName());

        Message message = new Message(doorPayload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, APP_DOOR_BLOCK.getKey());

        OutgoingRouter router = requireComponent(OutgoingRouter.KEY);
        router.sendMessageToMaster(MASTER_DOOR_LOCK_SET, message);
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
    public void refreshImage() {
        List<Module> cameras = requireComponent(AppModuleHandler.KEY).getCameras();
        if (cameras.size() < 1) {
            Log.e(TAG, "Could not refresh the door image. No camera avaliable");
            return;
        }
        CameraPayload payload = new CameraPayload(0, cameras.get(0).getName());

        Message message = new Message(payload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, APP_CAMERA_GET.getKey());

        OutgoingRouter router = requireComponent(OutgoingRouter.KEY);
        router.sendMessageToMaster(MASTER_CAMERA_GET, message);
    }

    /**
     * The listener interface to receive door events.
     */
    public interface DoorListener {
        void onPictureChanged(byte[] image);

        void onDoorStatusChanged();
    }
}
