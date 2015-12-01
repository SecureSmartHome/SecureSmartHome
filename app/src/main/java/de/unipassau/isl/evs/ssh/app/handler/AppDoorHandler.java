package de.unipassau.isl.evs.ssh.app.handler;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.app.activity.DoorFragment;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorPayload;

public class AppDoorHandler extends AbstractComponent implements MessageHandler {
    public static final Key<AppDoorHandler> KEY = new Key<>(AppDoorHandler.class);


    private boolean isDoorBlocked = false;
    private boolean isDoorOpen = false;
    private IncomingDispatcher dispatcher;
    private List<DoorFragment.ImageListener> listeners = new LinkedList<>();

    @Override
    public void handle(Message.AddressedMessage message) {
        String routingKey = message.getRoutingKey();
        if (routingKey.equals(CoreConstants.RoutingKeys.SLAVE_CAMERA_GET)) {
            CameraPayload payload = (CameraPayload) message.getPayload();
            notifyImageListeners(payload.getPicture());
        } else {
            throw new IllegalArgumentException("Unkown Routing Key: " + routingKey);
        }
    }

    public void addListener(DoorFragment.ImageListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DoorFragment.ImageListener listener) {
        listeners.remove(listener);
    }

    private void notifyImageListeners(byte[] image){
        for (DoorFragment.ImageListener listener : listeners) {
            listener.onPictureChanged(image);
        }
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void handlerRemoved(String routingKey) {
        dispatcher = null;
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
        isDoorOpen = true;
    }

    /**
     * Sends a "BlockDoor" message to the master.
     */
    public void blockDoor() {
        DoorPayload doorPayload = new DoorPayload(true);

        Message message;
        message = new Message(doorPayload);
        // TODO set RoutingKey
        message.putHeader(Message.HEADER_REPLY_TO_KEY, CoreConstants.RoutingKeys.MASTER_DOOR_RINGS);

        OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_LIGHT_SET, message);
        isDoorBlocked = true;
    }

    /**
     * Sends a "UnblockDoor" message to the master.
     */
    public void unblockDoor() {
        isDoorBlocked = false;
    }
}
