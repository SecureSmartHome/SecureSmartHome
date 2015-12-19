package de.unipassau.isl.evs.ssh.master.handler;

import java.util.Map;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorLockPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorStatusPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorUnlatchPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_DOOR_LOCK_GET;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_DOOR_LOCK_SET;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_DOOR_STATUS_GET;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_DOOR_UNLATCH;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.SLAVE_DOOR_STATUS_GET;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.SLAVE_DOOR_UNLATCH;

/**
 * Handles door messages and generates messages for each target and passes them to the OutgoingRouter.
 *
 * @author Leon Sell
 */
public class MasterDoorHandler extends AbstractMasterHandler {
    //Todo: formulate messages
    public static final String DOOR_UNLATCHED_MESSAGE = "Door unlatched";
    public static final String DOOR_UNLOCKED_MESSAGE = "Door unlocked";
    public static final String DOOR_LOCKED_MESSAGE = "Door locked";
    private Map<Integer, Boolean> lockedFor;

    @Override
    public void handle(Message.AddressedMessage message) {
        saveMessage(message);

        if (MASTER_DOOR_UNLATCH.matches(message)) {
            //Response or request?
            if (message.getHeader(Message.HEADER_REFERENCES_ID) == null) {
                //Request
                handleDoorUnlatch(message);
            } else {
                //Response
                handleDoorUnlatchResponse(message);
            }
        } else if (MASTER_DOOR_LOCK_SET.matches(message)) {
            handleDoorLockSet(message);
        } else if (MASTER_DOOR_LOCK_GET.matches(message)) {
            handleDoorLockGet(message);
        } else if (MASTER_DOOR_STATUS_GET.matches(message)) {
            //Response or request?
            if (message.getHeader(Message.HEADER_REFERENCES_ID) == null) {
                //Request
                handleDoorStatusGet(message);
            } else {
                //Response
                handleDoorStatusGetResponse(message);
            }
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            handleErrorMessage(message);
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_DOOR_UNLATCH, MASTER_DOOR_LOCK_SET, MASTER_DOOR_LOCK_GET, MASTER_DOOR_STATUS_GET};
    }

    private void handleDoorStatusGetResponse(Message.AddressedMessage message) {
        //Response
        final Message.AddressedMessage correspondingMessage =
                getMessageOnBehalfOfSequenceNr(message.getHeader(Message.HEADER_REFERENCES_ID));
        final Message messageToSend = new Message(MASTER_DOOR_STATUS_GET.getPayload(message));
        messageToSend.putHeader(Message.HEADER_REFERENCES_ID, correspondingMessage.getSequenceNr());

        //works for get and set, so no switch required
        sendMessage(
                correspondingMessage.getFromID(),
                correspondingMessage.getHeader(Message.HEADER_REPLY_TO_KEY),
                messageToSend
        );
    }

    private void handleDoorStatusGet(Message.AddressedMessage message) {
        DoorStatusPayload doorStatusPayload = MASTER_DOOR_STATUS_GET.getPayload(message);
        final Module atModule = requireComponent(SlaveController.KEY).getModule(doorStatusPayload.getModuleName());
        //Get status
        if (hasPermission(
                message.getFromID(),
                new Permission(
                        CoreConstants.Permission.BinaryPermission.REQUEST_DOOR_STATUS.toString(),
                        atModule.getName()
                )
        )) {
            final Message messageToSend = new Message(doorStatusPayload);
            messageToSend.putHeader(Message.HEADER_REPLY_TO_KEY, message.getRoutingKey());
            final Message.AddressedMessage sendMessage = sendMessage(
                    atModule.getAtSlave(),
                    SLAVE_DOOR_STATUS_GET,
                    messageToSend
            );
            putOnBehalfOf(sendMessage.getSequenceNr(), message.getSequenceNr());
        } else {
            //no permission
            sendErrorMessage(message);
        }
    }

    private void handleDoorLockGet(Message.AddressedMessage message) {
        DoorLockPayload doorLockPayload = MASTER_DOOR_LOCK_GET.getPayload(message);
        Module atModule = requireComponent(SlaveController.KEY)
                .getModule(doorLockPayload.getModuleName());

        if (requireComponent(PermissionController.KEY)
                .hasPermission(message.getFromID(), new Permission(
                        CoreConstants.Permission.BinaryPermission.REQUEST_DOOR_STATUS.toString(), atModule.getName()))) {


            Message messageToSend = new Message(new DoorLockPayload(getLocked(atModule.getName()), atModule.getName()));
            messageToSend.putHeader(Message.HEADER_REFERENCES_ID, message.getSequenceNr());

            sendMessage(message.getFromID(), message.getHeader(Message.HEADER_REPLY_TO_KEY), message);

        } else {
            //no permission
            sendErrorMessage(message);
        }
    }

    private void handleDoorLockSet(Message.AddressedMessage message) {
        DoorLockPayload doorLockPayload = MASTER_DOOR_LOCK_SET.getPayload(message);
        Module atModule = requireComponent(SlaveController.KEY)
                .getModule(doorLockPayload.getModuleName());

        if (requireComponent(PermissionController.KEY)
                .hasPermission(message.getFromID(), new Permission(
                        CoreConstants.Permission.BinaryPermission.LOCK_DOOR.toString(), atModule.getName()))) {

            setLocked(atModule.getName(), !doorLockPayload.isUnlock());

            //Send notification
            if (doorLockPayload.isUnlock()) {
                sendMessageLocal(
                        MASTER_NOTIFICATION_SEND,
                        new Message(
                                new NotificationPayload(
                                        CoreConstants.Permission.BinaryPermission.DOOR_UNLOCKED.toString(),
                                        DOOR_UNLOCKED_MESSAGE
                                )
                        ));
            } else {
                sendMessageLocal(
                        MASTER_NOTIFICATION_SEND,
                        new Message(
                                new NotificationPayload(
                                        CoreConstants.Permission.BinaryPermission.DOOR_LOCKED.toString(),
                                        DOOR_LOCKED_MESSAGE
                                )
                        ));
            }
        } else {
            //no permission
            sendErrorMessage(message);
        }
    }

    private void handleDoorUnlatchResponse(Message.AddressedMessage message) {
        Message.AddressedMessage correspondingMessage =
                getMessageOnBehalfOfSequenceNr(message.getHeader(Message.HEADER_REFERENCES_ID));
        Message messageToSend = new Message(new NotificationPayload(
                CoreConstants.Permission.BinaryPermission.DOOR_UNLATCHED.toString(), DOOR_UNLATCHED_MESSAGE));
        messageToSend.putHeader(Message.HEADER_REFERENCES_ID, correspondingMessage.getSequenceNr());

        sendMessageLocal(MASTER_NOTIFICATION_SEND, messageToSend);
    }

    private void handleDoorUnlatch(Message.AddressedMessage message) {
        DoorUnlatchPayload doorUnlatchPayload = MASTER_DOOR_UNLATCH.getPayload(message);
        Module atModule = requireComponent(SlaveController.KEY)
                .getModule(doorUnlatchPayload.getModuleName());
        Message messageToSend = new Message(doorUnlatchPayload);
        messageToSend.putHeader(Message.HEADER_REPLY_TO_KEY, message.getRoutingKey());

        if (requireComponent(PermissionController.KEY)
                .hasPermission(message.getFromID(), new Permission(
                        CoreConstants.Permission.BinaryPermission.UNLATCH_DOOR.toString(), atModule.getName()))) {

            if (!getLocked(atModule.getName())) {
                Message.AddressedMessage sendMessage =
                        sendMessage(
                                atModule.getAtSlave(),
                                SLAVE_DOOR_UNLATCH,
                                messageToSend
                        );
                putOnBehalfOf(sendMessage.getSequenceNr(), message.getSequenceNr());
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