package de.unipassau.isl.evs.ssh.master.handler;

import java.util.HashMap;
import java.util.Map;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBlockPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorStatusPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.network.NotificationBroadcaster;
import de.unipassau.isl.evs.ssh.master.task.MasterHolidaySimulationPlannerHandler;

import static de.unipassau.isl.evs.ssh.core.messaging.Message.HEADER_REFERENCES_ID;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_DOOR_STATUS_UPDATE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_BLOCK;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_STATUS_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_STATUS_UPDATE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_UNLATCH;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_DOOR_STATUS_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_DOOR_STATUS_GET_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_DOOR_UNLATCH;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_DOOR_UNLATCH_REPLY;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.LOCK_DOOR;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.REQUEST_DOOR_STATUS;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.UNLATCH_DOOR;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.UNLATCH_DOOR_ON_HOLIDAY;

/**
 * Handles door messages and generates messages for each target and passes them to the OutgoingRouter.
 *
 * @author Leon Sell
 */
public class MasterDoorHandler extends AbstractMasterHandler {
    private final Map<Integer, Boolean> blockedFor = new HashMap<>();
    private final Map<Integer, Boolean> openFor = new HashMap<>();

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                MASTER_DOOR_STATUS_UPDATE,
                MASTER_DOOR_UNLATCH,
                SLAVE_DOOR_UNLATCH_REPLY,
                MASTER_DOOR_BLOCK,
                MASTER_DOOR_STATUS_GET,
                SLAVE_DOOR_STATUS_GET_REPLY
        };
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_DOOR_STATUS_UPDATE.matches(message)) {
            handleDoorStatusUpdate(MASTER_DOOR_STATUS_UPDATE.getPayload(message));
        } else if (MASTER_DOOR_UNLATCH.matches(message)) {
            handleDoorUnlatchRequest(MASTER_DOOR_UNLATCH.getPayload(message), message);
        } else if (SLAVE_DOOR_UNLATCH_REPLY.matches(message)) {
            handleDoorUnlatchResponse(message);
        } else if (MASTER_DOOR_BLOCK.matches(message)) {
            handleDoorBlockSet(MASTER_DOOR_BLOCK.getPayload(message), message);
        } else if (MASTER_DOOR_STATUS_GET.matches(message)) {
            handleDoorStatusGet(message);
        } else if (SLAVE_DOOR_STATUS_GET_REPLY.matches(message)) {
            handleDoorStatusGetResponse(SLAVE_DOOR_STATUS_GET_REPLY.getPayload(message), message);
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            handleErrorMessage(message);
        } else {
            invalidMessage(message);
        }
    }

    private void handleDoorStatusUpdate(DoorStatusPayload payload) {
        setOpen(payload.getModuleName(), payload.isOpen());
        broadcastDoorStatus(payload.getModuleName());
    }

    private void handleDoorUnlatchRequest(DoorPayload payload, Message.AddressedMessage message) {
        final Module atModule = requireComponent(SlaveController.KEY).getModule(payload.getModuleName());
        final Message messageToSend = new Message(payload);

        if (hasPermission(message.getFromID(), UNLATCH_DOOR, null)) {
            if (!getBlocked(atModule.getName())) {
                Message.AddressedMessage sentMessage = sendMessage(atModule.getAtSlave(), SLAVE_DOOR_UNLATCH, messageToSend);
                recordReceivedMessageProxy(message, sentMessage);
            } else {
                sendReply(message, new Message(new ErrorPayload("Cannot unlatch door. Door is blocked.")));
            }
        } else if (hasPermission(message.getFromID(), UNLATCH_DOOR_ON_HOLIDAY, null)) {
            if (requireComponent(MasterHolidaySimulationPlannerHandler.KEY).isRunHolidaySimulation()) {

                if (!getBlocked(atModule.getName())) {
                    Message.AddressedMessage sentMessage = sendMessage(atModule.getAtSlave(), SLAVE_DOOR_UNLATCH, messageToSend);
                    recordReceivedMessageProxy(message, sentMessage);
                } else {
                    sendReply(message, new Message(new ErrorPayload("Cannot unlatch door. Door is blocked.")));
                }
            }
        } else {
            sendNoPermissionReply(message, UNLATCH_DOOR);
        }
    }

    private void handleDoorUnlatchResponse(Message.AddressedMessage message) {
        requireComponent(NotificationBroadcaster.KEY).sendMessageToAllReceivers(
                NotificationPayload.NotificationType.DOOR_UNLATCHED, message
        );

        final Message.AddressedMessage correspondingMessage = takeProxiedReceivedMessage(message.getHeader(HEADER_REFERENCES_ID));
        sendReply(correspondingMessage, new Message());
    }


    private void handleDoorStatusGetResponse(DoorStatusPayload payload, Message.AddressedMessage message) {
        final Message.AddressedMessage correspondingMessage = takeProxiedReceivedMessage(message.getHeader(HEADER_REFERENCES_ID));
        final Message messageToSend = new Message(payload);
        setOpen(payload.getModuleName(), payload.isOpen());
        sendReply(correspondingMessage, messageToSend);
    }

    private void handleDoorStatusGet(Message.AddressedMessage message) {
        final DoorPayload payload = MASTER_DOOR_STATUS_GET.getPayload(message);
        final Module atModule = requireComponent(SlaveController.KEY).getModule(payload.getModuleName());
        if (hasPermission(message.getFromID(), REQUEST_DOOR_STATUS, null)) {
            final Message messageToSlave = new Message(payload);
            final Message.AddressedMessage sendMessage = sendMessage(
                    atModule.getAtSlave(),
                    SLAVE_DOOR_STATUS_GET,
                    messageToSlave
            );
            recordReceivedMessageProxy(message, sendMessage);
        } else {
            sendNoPermissionReply(message, REQUEST_DOOR_STATUS);
        }
    }

    private void handleDoorBlockSet(DoorBlockPayload payload, Message.AddressedMessage message) {
        final Module atModule = requireComponent(SlaveController.KEY).getModule(payload.getModuleName());
        NotificationBroadcaster notificationBroadcaster = requireComponent(NotificationBroadcaster.KEY);

        if (hasPermission(message.getFromID(), LOCK_DOOR, null)) {
            setBlocked(atModule.getName(), payload.isLock());

            if (payload.isLock()) {
                notificationBroadcaster.sendMessageToAllReceivers(NotificationPayload.NotificationType.DOOR_LOCKED);
            } else {
                notificationBroadcaster.sendMessageToAllReceivers(NotificationPayload.NotificationType.DOOR_UNLOCKED);
            }
        } else {
            sendNoPermissionReply(message, LOCK_DOOR);
        }
    }

    private void broadcastDoorStatus(String moduleName) {
        boolean isOpen = getOpen(moduleName);
        boolean isBlocked = getBlocked(moduleName);
        Message messageToSend = new Message(new DoorStatusPayload(isOpen, isBlocked, moduleName));
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
        return openFor.get(requireComponent(SlaveController.KEY).getModuleID(moduleName));
    }

    private synchronized void setBlocked(String moduleName, boolean locked) {
        if (moduleName != null) {
            blockedFor.put(requireComponent(SlaveController.KEY).getModuleID(moduleName), locked);
        } else {
            throw new IllegalArgumentException("moduleName may not be null. Can't lock nonexistent Module.");
        }
    }

    private synchronized boolean getBlocked(String moduleName) {
        return blockedFor.get(requireComponent(SlaveController.KEY).getModuleID(moduleName));
    }
}