package de.unipassau.isl.evs.ssh.master.handler;

import java.util.HashMap;
import java.util.Map;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorLockPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorStatusPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorUnlatchPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.network.NotificationBroadcaster;

import static de.unipassau.isl.evs.ssh.core.messaging.Message.HEADER_REFERENCES_ID;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_LOCK_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_LOCK_GET_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_LOCK_SET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_STATUS_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_STATUS_GET_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_UNLATCH;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_DOOR_STATUS_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_DOOR_STATUS_GET_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_DOOR_UNLATCH;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_DOOR_UNLATCH_REPLY;

/**
 * Handles door messages and generates messages for each target and passes them to the OutgoingRouter.
 *
 * @author Leon Sell
 */
public class MasterDoorHandler extends AbstractMasterHandler {
    NotificationBroadcaster notificationBroadcaster = new NotificationBroadcaster();
    private final Map<Integer, Boolean> lockedFor = new HashMap<>();

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_DOOR_UNLATCH.matches(message)) {
            handleDoorUnlatch(message);
        } else if (SLAVE_DOOR_UNLATCH_REPLY.matches(message)) {
            notificationBroadcaster.sendMessageToAllReceivers(
                    NotificationPayload.NotificationType.DOOR_UNLATCHED, message
            );
        } else if (MASTER_DOOR_LOCK_SET.matches(message)) {
            handleDoorLockSet(message);
        } else if (MASTER_DOOR_LOCK_GET.matches(message)) {
            handleDoorLockGet(message);
        } else if (MASTER_DOOR_STATUS_GET.matches(message)) {
            handleDoorStatusGet(message);
        } else if (SLAVE_DOOR_STATUS_GET_REPLY.matches(message)) {
            handleDoorStatusGetResponse(message);
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            handleErrorMessage(message);
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                MASTER_DOOR_UNLATCH,
                SLAVE_DOOR_UNLATCH_REPLY,
                MASTER_DOOR_LOCK_SET,
                MASTER_DOOR_LOCK_GET,
                MASTER_DOOR_STATUS_GET,
                SLAVE_DOOR_STATUS_GET_REPLY
        };
    }

    private void handleDoorStatusGetResponse(Message.AddressedMessage message) {
        //Response
        final Message.AddressedMessage correspondingMessage =
                takeProxiedReceivedMessage(message.getHeader(HEADER_REFERENCES_ID));
        final Message messageToSend = new Message(MASTER_DOOR_STATUS_GET_REPLY.getPayload(message));
        messageToSend.putHeader(Message.HEADER_REFERENCES_ID, correspondingMessage.getSequenceNr());

        sendMessage(
                correspondingMessage.getFromID(),
                MASTER_DOOR_STATUS_GET_REPLY,
                messageToSend
        );
    }

    private void handleDoorStatusGet(Message.AddressedMessage message) {
        final DoorStatusPayload doorStatusPayload = MASTER_DOOR_STATUS_GET.getPayload(message);
        final Module atModule = requireComponent(SlaveController.KEY).getModule(doorStatusPayload.getModuleName());
        if (hasPermission(
                message.getFromID(),
                de.unipassau.isl.evs.ssh.core.sec.Permission.REQUEST_DOOR_STATUS,
                atModule.getName()
        )) {
            final Message messageToSend = new Message(doorStatusPayload);
            final Message.AddressedMessage sendMessage = sendMessage(
                    atModule.getAtSlave(),
                    SLAVE_DOOR_STATUS_GET,
                    messageToSend
            );
            recordReceivedMessageProxy(message, sendMessage);
        } else {
            //no permission
            sendErrorMessage(message);
        }
    }

    private void handleDoorLockGet(Message.AddressedMessage message) {
        final DoorLockPayload doorLockPayload = MASTER_DOOR_LOCK_GET.getPayload(message);
        final Module atModule = requireComponent(SlaveController.KEY)
                .getModule(doorLockPayload.getModuleName());

        if (hasPermission(
                message.getFromID(),
                de.unipassau.isl.evs.ssh.core.sec.Permission.REQUEST_DOOR_STATUS,
                atModule.getName()
        )) {
            final Message messageToSend =
                    new Message(new DoorLockPayload(getLocked(atModule.getName()), atModule.getName()));
            messageToSend.putHeader(Message.HEADER_REFERENCES_ID, message.getSequenceNr());

            sendMessage(
                    message.getFromID(),
                    MASTER_DOOR_LOCK_GET_REPLY,
                    message
            );
        } else {
            //no permission
            sendErrorMessage(message);
        }
    }

    private void handleDoorLockSet(Message.AddressedMessage message) {
        final DoorLockPayload doorLockPayload = MASTER_DOOR_LOCK_SET.getPayload(message);
        final Module atModule = requireComponent(SlaveController.KEY)
                .getModule(doorLockPayload.getModuleName());

        if (hasPermission(
                message.getFromID(),
                de.unipassau.isl.evs.ssh.core.sec.Permission.LOCK_DOOR,
                atModule.getName()
        )) {
            setLocked(atModule.getName(), !doorLockPayload.isUnlock());

            if (doorLockPayload.isUnlock()) {
                notificationBroadcaster.sendMessageToAllReceivers(NotificationPayload.NotificationType.DOOR_UNLOCKED);
            } else {
                notificationBroadcaster.sendMessageToAllReceivers(NotificationPayload.NotificationType.DOOR_LOCKED);
            }
        } else {
            //no permission
            sendErrorMessage(message);
        }
    }

    private void handleDoorUnlatch(Message.AddressedMessage message) {
        final DoorUnlatchPayload doorUnlatchPayload = MASTER_DOOR_UNLATCH.getPayload(message);
        final Module atModule = requireComponent(SlaveController.KEY)
                .getModule(doorUnlatchPayload.getModuleName());
        final Message messageToSend = new Message(doorUnlatchPayload);

        if (hasPermission(message.getFromID(),
                de.unipassau.isl.evs.ssh.core.sec.Permission.UNLATCH_DOOR,
                atModule.getName()
        )) {
            if (!getLocked(atModule.getName())) {
                Message.AddressedMessage sendMessage =
                        sendMessage(
                                atModule.getAtSlave(),
                                SLAVE_DOOR_UNLATCH,
                                messageToSend
                        );
                recordReceivedMessageProxy(message, sendMessage);
            } else {
                //locked
                sendErrorMessage(message);
            }
        } else {
            //no permission
            sendErrorMessage(message);
        }
    }

    private synchronized void setLocked(String moduleName, boolean locked) {
        if (moduleName != null) {
            lockedFor.put(requireComponent(SlaveController.KEY).getModuleID(moduleName), locked);
        } else {
            throw new IllegalArgumentException("moduleName may not be null. Can't lock nonexistent Module.");
        }
    }

    private synchronized boolean getLocked(String moduleName) {
        return lockedFor.get(requireComponent(SlaveController.KEY).getModuleID(moduleName));
    }
}