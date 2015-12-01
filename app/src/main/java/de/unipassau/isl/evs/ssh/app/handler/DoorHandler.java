package de.unipassau.isl.evs.ssh.app.handler;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

public class DoorHandler extends AbstractComponent implements MessageHandler {
    public static final Key<DoorHandler> KEY = new Key<>(DoorHandler.class);


    private boolean isDoorBlocked = false;
    private boolean isDoorOpen = false;
    private IncomingDispatcher dispatcher;

    @Override
    public void handle(Message.AddressedMessage message) {

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
        isDoorBlocked = true;
    }

    /**
     * Sends a "UnblockDoor" message to the master.
     */
    public void unblockDoor() {
        isDoorBlocked = false;
    }
}
