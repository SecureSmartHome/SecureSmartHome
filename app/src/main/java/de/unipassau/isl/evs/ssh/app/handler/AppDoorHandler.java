package de.unipassau.isl.evs.ssh.app.handler;

import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.app.activity.DoorFragment;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorLockPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorStatusPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorUnlatchPayload;

/**
 * The AppDoorHandler class sends and receives messages for door events.
 *
 * @author Wolfgang Popp
 */
public class AppDoorHandler extends AbstractComponent implements MessageHandler {
    public static final Key<AppDoorHandler> KEY = new Key<>(AppDoorHandler.class);
    private static final String TAG = AppDoorHandler.class.getSimpleName();

    private boolean isDoorBlocked = false;
    private boolean isDoorOpen = false;
    private List<DoorFragment.DoorListener> listeners = new LinkedList<>();

    /**
     * Adds a DoorListener to this handler.
     *
     * @param listener
     */
    public void addListener(DoorFragment.DoorListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the given DoorListener from this handler.
     *
     * @param listener
     */
    public void removeListener(DoorFragment.DoorListener listener) {
        listeners.remove(listener);
    }

    private void fireImageUpdated(byte[] image) {
        if (image == null) {
            Log.v(TAG, "No camera picture came with the message.");
        } else {
            Log.v(TAG, "Received picture.");
        }
        for (DoorFragment.DoorListener listener : listeners) {
            listener.onPictureChanged(image);
        }
    }

    private void fireStatusUpdated() {
        for (DoorFragment.DoorListener listener : listeners) {
            listener.onDoorStatusChanged();
        }
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
    }

    @Override
    public void handlerRemoved(String routingKey) {
    }

    @Override
    public void init(Container container) {
        super.init(container);
        getContainer().require(IncomingDispatcher.KEY).registerHandler(this,
                CoreConstants.RoutingKeys.APP_CAMERA_GET,
                CoreConstants.RoutingKeys.APP_DOOR_BLOCK,
                CoreConstants.RoutingKeys.APP_DOOR_GET,
                CoreConstants.RoutingKeys.APP_DOOR_RING);
    }

    @Override
    public void destroy() {
        getContainer().require(IncomingDispatcher.KEY).unregisterHandler(this,
                CoreConstants.RoutingKeys.APP_CAMERA_GET,
                CoreConstants.RoutingKeys.APP_DOOR_BLOCK,
                CoreConstants.RoutingKeys.APP_DOOR_GET,
                CoreConstants.RoutingKeys.APP_DOOR_RING);
        super.destroy();
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        String routingKey = message.getRoutingKey();
        if (routingKey.equals(CoreConstants.RoutingKeys.APP_CAMERA_GET)) {
            CameraPayload payload = (CameraPayload) message.getPayload();
            fireImageUpdated(payload.getPicture());
        } else if (routingKey.equals(CoreConstants.RoutingKeys.APP_DOOR_BLOCK)) {
            DoorLockPayload payload = (DoorLockPayload) message.getPayload();
            isDoorBlocked = !payload.isUnlock();
            fireStatusUpdated();
        } else if (routingKey.equals(CoreConstants.RoutingKeys.APP_DOOR_GET)) {
            DoorStatusPayload payload = (DoorStatusPayload) message.getPayload();
            isDoorOpen = !payload.isClosed();
            fireStatusUpdated();
        } else if (routingKey.equals(CoreConstants.RoutingKeys.APP_DOOR_RING)) {
            DoorBellPayload doorBellPayload = (DoorBellPayload) message.getPayload();
            fireImageUpdated(doorBellPayload.getCameraPayload().getPicture());
            Message messageToSend = new Message(doorBellPayload);
            requireComponent(OutgoingRouter.KEY).sendMessageLocal(CoreConstants.RoutingKeys.APP_NOTIFICATION_RECEIVE,
                    messageToSend);
        } else {
            throw new IllegalArgumentException("Unkown Routing Key: " + routingKey);
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
    public void refresh(){
        List<Module> doors = getContainer().require(AppModuleHandler.KEY).getDoors();
        if (doors == null) {
            Log.e(TAG, "Could not get door status. No door installed");
            return;
        }
        refreshBlockStatus(doors.get(0).getName());
        refreshOpenStatus(doors.get(0).getName());
    }

    private void refreshOpenStatus(String door) {
        DoorStatusPayload doorPayload = new DoorStatusPayload(door);

        Message message = new Message(doorPayload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, CoreConstants.RoutingKeys.APP_DOOR_GET);

        OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_DOOR_STATUS_GET, message);

    }

    private void refreshBlockStatus(String door) {
        DoorUnlatchPayload doorPayload = new DoorUnlatchPayload(door);

        Message message = new Message(doorPayload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, CoreConstants.RoutingKeys.APP_DOOR_BLOCK);

        OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_DOOR_LOCK_GET, message);
    }

    /**
     * Sends a "OpenDoor" message to the master.
     */
    public void openDoor() {
        List<Module> doors = getContainer().require(AppModuleHandler.KEY).getDoors();
        if (doors == null) {
            Log.e(TAG, "Could not open the door. No door installed");
            return;
        }
        DoorUnlatchPayload doorPayload = new DoorUnlatchPayload(doors.get(0).getName());

        Message message = new Message(doorPayload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, CoreConstants.RoutingKeys.APP_DOOR_GET);

        OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_DOOR_UNLATCH, message);
        isDoorOpen = true;
    }

    private void blockDoor(boolean isBlocked) {
        List<Module> doors = getContainer().require(AppModuleHandler.KEY).getDoors();
        if (doors == null) {
            Log.e(TAG, "Could not (un)block the door. No door installed");
            return;
        }
        DoorLockPayload doorPayload = new DoorLockPayload(isBlocked, doors.get(0).getName());

        Message message = new Message(doorPayload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, CoreConstants.RoutingKeys.APP_DOOR_BLOCK);

        OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_DOOR_LOCK_SET, message);
        isDoorBlocked = isBlocked;
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
        List<Module> cameras = getContainer().require(AppModuleHandler.KEY).getCameras();
        if (cameras == null) {
            Log.e(TAG, "Could not refresh the door image. No camera avaliable");
            return;
        }
        CameraPayload payload = new CameraPayload(0, cameras.get(0).getName());

        Message message = new Message(payload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, CoreConstants.RoutingKeys.APP_CAMERA_GET);

        OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_CAMERA_GET, message);
    }
}
