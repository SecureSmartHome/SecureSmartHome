package de.unipassau.isl.evs.ssh.master.handler;

import java.util.HashMap;
import java.util.Map;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.NoPermissionException;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBlockPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorStatusPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.network.broadcast.NotificationBroadcaster;
import de.unipassau.isl.evs.ssh.master.task.MasterHolidaySimulationPlannerHandler;

import static de.unipassau.isl.evs.ssh.core.messaging.Message.HEADER_REFERENCES_ID;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_DOOR_STATUS_UPDATE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_BLOCK;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_STATUS_UPDATE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_UNLATCH;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_DOOR_UNLATCH;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_DOOR_UNLATCH_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_DOOR_UNLATCH_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.DOOR_UNLATCHED;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.LOCK_DOOR;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.REQUEST_DOOR_STATUS;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.UNLATCH_DOOR;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.UNLATCH_DOOR_ON_HOLIDAY;

/**
 * Handles door messages and generates messages for each target and passes them to the OutgoingRouter.
 *
 * @author Leon Sell
 * @author Wolfgang Popp
 */
public class MasterDoorHandler extends AbstractMasterHandler {
    private final Map<Integer, Boolean> blockedFor = new HashMap<>();
    private final Map<Integer, Boolean> openFor = new HashMap<>();

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                MASTER_DOOR_GET,
                MASTER_DOOR_STATUS_UPDATE,
                MASTER_DOOR_UNLATCH,
                MASTER_DOOR_BLOCK,
                SLAVE_DOOR_UNLATCH_REPLY,
                SLAVE_DOOR_UNLATCH_ERROR,
        };
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_DOOR_GET.matches(message)) {
            handleDoorGetRequest(MASTER_DOOR_GET.getPayload(message), message);
        } else if (MASTER_DOOR_STATUS_UPDATE.matches(message)) {
            handleDoorStatusUpdate(MASTER_DOOR_STATUS_UPDATE.getPayload(message));
        } else if (MASTER_DOOR_UNLATCH.matches(message)) {
            handleDoorUnlatchRequest(message, MASTER_DOOR_UNLATCH.getPayload(message));
        } else if (SLAVE_DOOR_UNLATCH_REPLY.matches(message)) {
            handleDoorUnlatchResponse(SLAVE_DOOR_UNLATCH_REPLY.getPayload(message), message);
        } else if (MASTER_DOOR_BLOCK.matches(message)) {
            handleDoorBlockSet(message, MASTER_DOOR_BLOCK.getPayload(message));
        } else if (SLAVE_DOOR_UNLATCH_ERROR.matches(message)) {
            replyError(message, SLAVE_DOOR_UNLATCH_ERROR);
        } else {
            invalidMessage(message);
        }
    }

    private void handleDoorGetRequest(DoorPayload payload, Message.AddressedMessage original) {
        if (!hasPermission(original.getFromID(), REQUEST_DOOR_STATUS)) {
            sendReply(original, new Message(new ErrorPayload(new NoPermissionException(REQUEST_DOOR_STATUS))));
            return;
        }

        final String moduleName = payload.getModuleName();
        final boolean isOpen = getOpen(moduleName);
        final boolean isBlocked = getBlocked(moduleName);
        final Message reply = new Message(new DoorStatusPayload(isOpen, isBlocked, moduleName));
        sendReply(original, reply);
    }

    private void replyError(Message.AddressedMessage message, RoutingKey<ErrorPayload> routingKey) {
        Message messageToSend = new Message(routingKey.getPayload(message));
        Message.AddressedMessage original = takeProxiedReceivedMessage(message.getHeader(HEADER_REFERENCES_ID));
        sendReply(original, messageToSend);
    }

    private void handleDoorStatusUpdate(DoorStatusPayload payload) {
        setOpen(payload.getModuleName(), payload.isOpen());
        broadcastDoorStatus(payload.getModuleName());
    }

    private void handleDoorUnlatchRequest(Message.AddressedMessage message, DoorPayload payload) {
        final Module atModule = requireComponent(SlaveController.KEY).getModule(payload.getModuleName());
        final Message messageToSend = new Message(payload);

        if (hasPermission(message.getFromID(), UNLATCH_DOOR)) {
            if (!getBlocked(atModule.getName())) {
                final Message.AddressedMessage sentMessage =
                        sendMessage(atModule.getAtSlave(), SLAVE_DOOR_UNLATCH, messageToSend);
                recordReceivedMessageProxy(message, sentMessage);
            } else {
                sendReply(message, new Message(new ErrorPayload("Cannot unlatch door. Door is blocked.")));
            }
        } else if (hasPermission(message.getFromID(), UNLATCH_DOOR_ON_HOLIDAY)) {
            if (requireComponent(MasterHolidaySimulationPlannerHandler.KEY).isRunHolidaySimulation()) {
                if (!getBlocked(atModule.getName())) {
                    final Message.AddressedMessage sentMessage =
                            sendMessage(atModule.getAtSlave(), SLAVE_DOOR_UNLATCH, messageToSend);
                    recordReceivedMessageProxy(message, sentMessage);
                } else {
                    sendReply(message, new Message(new ErrorPayload("Cannot unlatch door. Door is blocked.")));
                }
            }
        } else {
            sendNoPermissionReply(message, UNLATCH_DOOR);
        }
    }

    private void handleDoorUnlatchResponse(DoorPayload payload, Message.AddressedMessage message) {
        requireComponent(NotificationBroadcaster.KEY).sendMessageToAllReceivers(DOOR_UNLATCHED, message);
        final Message.AddressedMessage correspondingMessage =
                takeProxiedReceivedMessage(message.getHeader(HEADER_REFERENCES_ID));
        sendReply(correspondingMessage, new Message());
        broadcastDoorStatus(payload.getModuleName());
    }

    private void handleDoorBlockSet(Message.AddressedMessage message, DoorBlockPayload payload) {
        final Module atModule = requireComponent(SlaveController.KEY).getModule(payload.getModuleName());
        final NotificationBroadcaster notificationBroadcaster = requireComponent(NotificationBroadcaster.KEY);

        if (hasPermission(message.getFromID(), LOCK_DOOR)) {
            setBlocked(atModule.getName(), payload.isBlock());

            if (payload.isBlock()) {
                notificationBroadcaster.sendMessageToAllReceivers(NotificationPayload.NotificationType.DOOR_LOCKED);
            } else {
                notificationBroadcaster.sendMessageToAllReceivers(NotificationPayload.NotificationType.DOOR_UNLOCKED);
            }
            broadcastDoorStatus(atModule.getName());
        } else {
            sendNoPermissionReply(message, LOCK_DOOR);
        }
    }

    private void broadcastDoorStatus(String moduleName) {
        boolean isOpen = getOpen(moduleName);
        boolean isBlocked = getBlocked(moduleName);
        final Message messageToSend = new Message(new DoorStatusPayload(isOpen, isBlocked, moduleName));
        sendMessageToAllDevicesWithPermission(messageToSend, REQUEST_DOOR_STATUS, null, APP_DOOR_STATUS_UPDATE);
    }

    private void setOpen(String moduleName, boolean isOpen) {
        if (moduleName != null) {
            openFor.put(requireComponent(SlaveController.KEY).getModuleID(moduleName), isOpen);
        } else {
            throw new IllegalArgumentException("moduleName may not be null. Can't lock nonexistent Module.");
        }
    }

    private Boolean getOpen(String moduleName) {
        final Boolean isOpen = openFor.get(requireComponent(SlaveController.KEY).getModuleID(moduleName));
        if (isOpen != null) {
            return isOpen;
        } else {
            return false;
        }
    }

    private synchronized void setBlocked(String moduleName, boolean locked) {
        if (moduleName != null) {
            blockedFor.put(requireComponent(SlaveController.KEY).getModuleID(moduleName), locked);
        } else {
            throw new IllegalArgumentException("moduleName may not be null. Can't lock nonexistent Module.");
        }
    }

    private synchronized boolean getBlocked(String moduleName) {
        final Boolean isBlocked = blockedFor.get(requireComponent(SlaveController.KEY).getModuleID(moduleName));
        if (isBlocked != null) {
            return isBlocked;
        } else {
            return true;
        }
    }
}